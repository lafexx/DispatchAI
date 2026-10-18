package com.dispatchai.dispatch.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.exception.DeliveryNotAssignableException;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.messaging.publisher.AssignmentEventPublisher;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentLifecycleJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.store.DeliveryAssignmentLifecycleStore;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@DataJpaTest(showSql = false)
@Import({ DispatchApplicationService.class, DeliveryAssignmentLifecycleStore.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DispatchAssignmentLifecycleIntegrationTest {
  @Autowired
  DispatchApplicationService service;
  @Autowired
  DeliveryAssignmentJpaRepository assignments;
  @Autowired
  DeliveryAssignmentLifecycleJpaRepository lifecycles;
  @MockitoBean
  FleetClient fleet;
  @MockitoBean
  DeliveryClient delivery;
  @MockitoBean
  AssignmentEventPublisher publisher;
  @MockitoBean
  ReservationCleanupService cleanup;

  @BeforeEach
  void defaults() {
    lenient().when(fleet.getVehicles()).thenReturn(java.util.List.of(new VehicleResponse(
        UUID.fromString("00000000-0000-0000-0000-000000000001"), "TEST", "VAN", java.math.BigDecimal.valueOf(1200), true)));

    when(delivery.getDelivery(any())).thenAnswer(call -> new DeliveryResponse(call.getArgument(0),
        UUID.randomUUID(), null, "Pickup", "Destination", RemoteDeliveryStatus.PENDING));
    when(fleet.reserve(any(), any())).thenAnswer(call -> {
      ReservationRequest request = call.getArgument(1);
      return new ReservationResponse(call.getArgument(0), request.deliveryId(), request.driverId(), request.vehicleId(),
          "RESERVED", Instant.now().plusSeconds(120));
    });
  }

  @Test
  void completionUpdatesAssignmentAndPreservesFirstTimestamp() {
    var deliveryId = UUID.randomUUID();
    service.create(new CreateAssignmentRequest(deliveryId, UUID.randomUUID(), null));
    var at = Instant.parse("2026-10-12T01:00:00Z");
    service.closeAssignment(deliveryId, AssignmentStatus.COMPLETED, at);
    service.closeAssignment(deliveryId, AssignmentStatus.COMPLETED, at.plusSeconds(1));
    var result = service.getByDeliveryId(deliveryId);
    assertEquals(AssignmentStatus.COMPLETED, result.status());
    assertEquals(at, result.completedAt());
    assertNull(result.cancelledAt());
  }

  @Test
  void cancellationUpdatesAssignment() {
    var deliveryId = UUID.randomUUID();
    service.create(new CreateAssignmentRequest(deliveryId, UUID.randomUUID(), null));
    var at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    service.closeAssignment(deliveryId, AssignmentStatus.CANCELLED, at);
    assertEquals(AssignmentStatus.CANCELLED, service.getByDeliveryId(deliveryId).status());
    assertEquals(at, service.getByDeliveryId(deliveryId).cancelledAt());
  }

  @Test
  void cancellationWithoutAssignmentIsRememberedAndBlocksStaleCreate() {
    var deliveryId = UUID.randomUUID();
    service.closeAssignment(deliveryId, AssignmentStatus.CANCELLED,
        Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    // Even if a REST read would return stale PENDING, local terminal knowledge
    // wins.
    assertThrows(DeliveryNotAssignableException.class,
        () -> service.create(new CreateAssignmentRequest(deliveryId, UUID.randomUUID(), null)));
    assertTrue(assignments.findByDeliveryId(deliveryId).isEmpty());
    assertTrue(lifecycles.findById(deliveryId).orElseThrow().isTerminal());
    verifyNoInteractions(publisher);
  }

  @Test
  void completionWaitsForInFlightCreationAndClosesTheCommittedAssignment() throws Exception {
    var deliveryId = UUID.randomUUID();
    var driver = UUID.randomUUID();
    var at = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    var reachedFleet = new CountDownLatch(1);
    var resumeCreate = new CountDownLatch(1);
    var closeStarted = new CountDownLatch(1);
    doAnswer(call -> {
      reachedFleet.countDown();
      if (!resumeCreate.await(5, TimeUnit.SECONDS)) {
        throw new IllegalStateException("Test timed out");
      }
      ReservationRequest request = call.getArgument(1);
      return new ReservationResponse(call.getArgument(0), request.deliveryId(), request.driverId(), request.vehicleId(),
          "RESERVED", Instant.now().plusSeconds(120));
    }).when(fleet).reserve(any(), any());
    try (var executor = Executors.newFixedThreadPool(2)) {
      var create = executor.submit(() -> service.create(new CreateAssignmentRequest(deliveryId, driver, null)));
      assertTrue(reachedFleet.await(5, TimeUnit.SECONDS));
      var close = executor.submit(() -> {
        closeStarted.countDown();
        service.closeAssignment(deliveryId, AssignmentStatus.CANCELLED, at);
      });
      try {
        assertTrue(closeStarted.await(5, TimeUnit.SECONDS));
        assertThrows(TimeoutException.class, () -> close.get(100, TimeUnit.MILLISECONDS));
      } finally {
        resumeCreate.countDown();
      }
      create.get(10, TimeUnit.SECONDS);
      close.get(10, TimeUnit.SECONDS);
    }
    assertEquals(AssignmentStatus.CANCELLED, service.getByDeliveryId(deliveryId).status());
    assertEquals(at, service.getByDeliveryId(deliveryId).cancelledAt());
  }
}
