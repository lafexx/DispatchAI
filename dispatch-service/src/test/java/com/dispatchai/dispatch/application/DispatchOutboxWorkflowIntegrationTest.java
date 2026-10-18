package com.dispatchai.dispatch.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.messaging.consumer.AssignmentLifecycleEventConsumer;
import com.dispatchai.dispatch.infrastructure.messaging.publisher.AssignmentEventPublisher;
import com.dispatchai.dispatch.infrastructure.messaging.event.AssignmentRejectedEvent;
import com.dispatchai.dispatch.infrastructure.messaging.outbox.OutboxWriter;
import com.dispatchai.dispatch.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.dispatch.infrastructure.persistence.repository.OutboxJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.store.DeliveryAssignmentLifecycleStore;
import com.dispatchai.dispatch.infrastructure.persistence.repository.ReservationCleanupJpaRepository;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({DispatchApplicationService.class, DeliveryAssignmentLifecycleStore.class, ReservationCleanupService.class,
    AssignmentEventPublisher.class, OutboxWriter.class, InboxProcessor.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DispatchOutboxWorkflowIntegrationTest {
  @Autowired DispatchApplicationService service;
  @Autowired DeliveryAssignmentJpaRepository assignments;
  @Autowired ReservationCleanupJpaRepository cleanups;
  @Autowired OutboxJpaRepository outbox;
  @Autowired InboxProcessor inbox;
  @MockitoBean FleetClient fleet;
  @MockitoBean DeliveryClient delivery;
  @MockitoBean ObjectMapper mapper;
  @BeforeEach void defaults() {
    lenient().when(fleet.getVehicles()).thenReturn(java.util.List.of(new VehicleResponse(
        UUID.fromString("00000000-0000-0000-0000-000000000001"), "TEST", "VAN", java.math.BigDecimal.valueOf(1200), true)));

    when(mapper.writeValueAsString(any())).thenReturn("{\"test\":true}");
    when(delivery.getDelivery(any())).thenAnswer(call -> new DeliveryResponse(call.getArgument(0),
        UUID.randomUUID(), null, "Pickup", "Destination", RemoteDeliveryStatus.PENDING));
    when(fleet.reserve(any(), any())).thenAnswer(call -> {
      ReservationRequest request = call.getArgument(1);
      return new ReservationResponse(call.getArgument(0), request.deliveryId(), request.driverId(), request.vehicleId(),
          "RESERVED", Instant.now().plusSeconds(120));
    });
  }
  @Test void httpCreateCommitsAnAssignmentAndCommandWithoutKafka() {
    var result = service.create(new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null));
    var assignment = assignments.findById(result.id()).orElseThrow();
    var message = outbox.findByEventId(assignment.getAssignmentEventId()).orElseThrow();
    assertNull(message.getPublishedAt()); assertEquals("driverAssigned-out-0", message.getBindingName());
    assertEquals(result.deliveryId(), message.getAggregateId());
  }
  @Test void serializationFailureRollsBackAssignmentAndCompensatesFleet() {
    when(mapper.writeValueAsString(any())).thenThrow(new IllegalStateException("Cannot serialize"));
    var id = UUID.randomUUID();
    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateAssignmentRequest(id, UUID.randomUUID(), null)));
    assertTrue(assignments.findByDeliveryId(id).isEmpty());
    assertTrue(outbox.findAll().stream().noneMatch(e -> id.equals(e.getAggregateId())));
    verify(fleet).release(any(), any());
  }
  @Test void rejectionAndCleanupIntentCommitWithInboxAndDuplicateDoesNotRecreateIntent() {
    var request = new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null);
    var assigned = service.create(request);
    var event = new AssignmentRejectedEvent(UUID.randomUUID(), assigned.id(), request.deliveryId(), request.driverId(),
        null, "Expired", "PENDING", Instant.now());
    var consumer = new AssignmentLifecycleEventConsumer().assignmentRejected(service, inbox);
    var message = MessageBuilder.withPayload(event).build();
    consumer.accept(message);
    assertEquals(AssignmentStatus.REJECTED, assignments.findById(assigned.id()).orElseThrow().getStatus());
    assertTrue(cleanups.existsById(assigned.id())); verify(fleet, never()).release(any(), any());
    // Once cleanup succeeds, replay must not recreate it.
    cleanups.deleteById(assigned.id()); consumer.accept(message);
    assertFalse(cleanups.existsById(assigned.id()));
  }
}
