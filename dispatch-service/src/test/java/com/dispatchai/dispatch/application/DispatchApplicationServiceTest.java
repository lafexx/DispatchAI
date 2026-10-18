package com.dispatchai.dispatch.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignmentLifecycle;
import com.dispatchai.dispatch.domain.exception.DeliveryNotAssignableException;
import com.dispatchai.dispatch.domain.exception.AssignmentNotFoundException;
import com.dispatchai.dispatch.domain.exception.NoAvailableDriverException;
import com.dispatchai.dispatch.domain.exception.ReservationConflictException;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.DriverResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.messaging.event.DriverAssignedEvent;
import com.dispatchai.dispatch.infrastructure.messaging.publisher.AssignmentEventPublisher;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.store.DeliveryAssignmentLifecycleStore;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@ExtendWith(MockitoExtension.class)
class DispatchApplicationServiceTest {
  @Mock FleetClient fleet;
  @Mock DeliveryClient deliveries;
  @Mock DeliveryAssignmentJpaRepository repository;
  @Mock AssignmentEventPublisher publisher;
  @Mock DeliveryAssignmentLifecycleStore lifecycles;
  @Mock ReservationCleanupService cleanup;
  @Mock PlatformTransactionManager manager;
  @InjectMocks DispatchApplicationService service;
  private final Map<UUID, DeliveryAssignment> saved = new HashMap<>();
  private final UUID customer = UUID.randomUUID();
  private DeliveryResponse pending(UUID id) { return new DeliveryResponse(id, customer, null, "Pickup", "Destination", RemoteDeliveryStatus.PENDING); }

  @BeforeEach void defaults() {
    lenient().when(fleet.getVehicles()).thenReturn(java.util.List.of(new VehicleResponse(
        UUID.fromString("00000000-0000-0000-0000-000000000001"), "TEST", "VAN", java.math.BigDecimal.valueOf(1200), true)));

    lenient().when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    lenient().when(lifecycles.lock(any())).thenAnswer(call -> new DeliveryAssignmentLifecycle(call.getArgument(0)));
    lenient().when(deliveries.getDelivery(any())).thenAnswer(call -> pending(call.getArgument(0)));
    lenient().when(fleet.reserve(any(), any())).thenAnswer(call -> {
      ReservationRequest request = call.getArgument(1);
      return new ReservationResponse(call.getArgument(0), request.deliveryId(), request.driverId(), request.vehicleId(), "RESERVED", Instant.now().plusSeconds(120));
    });
    lenient().when(repository.saveAndFlush(any())).thenAnswer(call -> {
      DeliveryAssignment assignment = call.getArgument(0); saved.put(assignment.getId(), assignment); return assignment;
    });
    lenient().when(repository.findById(any())).thenAnswer(call -> Optional.ofNullable(saved.get(call.getArgument(0))));
    lenient().when(repository.findFirstByDeliveryIdAndStatusOrderByAssignedAtDesc(any(), any())).thenAnswer(call ->
        saved.values().stream().filter(a -> a.getDeliveryId().equals(call.getArgument(0)) && a.getStatus() == call.getArgument(1)).findFirst());
    lenient().when(repository.findByDeliveryIdAndStatus(any(), any())).thenAnswer(call ->
        saved.values().stream().filter(a -> a.getDeliveryId().equals(call.getArgument(0)) && a.getStatus() == call.getArgument(1)).toList());
    lenient().when(repository.findByDeliveryId(any())).thenAnswer(call ->
        saved.values().stream().filter(a -> a.getDeliveryId().equals(call.getArgument(0))).max(Comparator.comparing(DeliveryAssignment::getAssignedAt)));
    lenient().when(repository.findByDriverId(any())).thenAnswer(call ->
        saved.values().stream().filter(a -> a.getDriverId().equals(call.getArgument(0))).toList());
  }

  @Test void createReservesAndEnqueuesBeforeCommit() {
    var delivery = UUID.randomUUID(); var driver = UUID.randomUUID(); var vehicle = UUID.randomUUID();
    when(fleet.getVehicles()).thenReturn(List.of(new VehicleResponse(vehicle,"EXPLICIT","VAN",java.math.BigDecimal.valueOf(1200),true)));
    var result = service.create(new CreateAssignmentRequest(delivery, driver, vehicle));
    assertEquals(AssignmentStatus.ACTIVE, result.status());
    var order = inOrder(fleet, repository, manager, publisher);
    order.verify(fleet).reserve(eq(result.id()), eq(new ReservationRequest(delivery, driver, vehicle)));
    order.verify(repository).saveAndFlush(any()); order.verify(publisher).publish(any(DriverAssignedEvent.class)); order.verify(manager).commit(any());
    var event = ArgumentCaptor.forClass(DriverAssignedEvent.class); verify(publisher).publish(event.capture());
    assertEquals(customer, event.getValue().customerId()); assertEquals(result.id(), event.getValue().assignmentId());
  }

  @Test void duplicateManualRequestReusesSameAssignmentAndDoesNotReserveAgain() {
    var request = new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null);
    var first = service.create(request); var second = service.create(request);
    assertEquals(first.id(), second.id()); verify(fleet, times(1)).reserve(any(), any());
    verify(publisher, times(1)).publish(any(DriverAssignedEvent.class));
  }

  @Test void conflictingActiveRequestIsRejectedWithoutReplacingIt() {
    var delivery = UUID.randomUUID(); var first = service.create(new CreateAssignmentRequest(delivery, UUID.randomUUID(), null));
    assertThrows(ReservationConflictException.class, () -> service.create(new CreateAssignmentRequest(delivery, UUID.randomUUID(), null)));
    assertEquals(1, saved.size()); assertEquals(AssignmentStatus.ACTIVE, service.getByAssignmentId(first.id()).status());
  }

  @Test void unavailableResourceConflictDoesNotPersistOrPublish() {
    doThrow(new ReservationConflictException("Driver busy")).when(fleet).reserve(any(), any());
    assertThrows(ReservationConflictException.class,
        () -> service.create(new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null)));
    assertTrue(saved.isEmpty()); verifyNoInteractions(publisher); verify(cleanup).compensate(any(), any());
  }

  @Test void cancellationWinningDuringReserveTriggersCompensation() {
    var delivery = UUID.randomUUID();
    when(deliveries.getDelivery(delivery)).thenReturn(pending(delivery),
        new DeliveryResponse(delivery, customer, null, "Pickup", "Destination", RemoteDeliveryStatus.CANCELLED));
    assertThrows(DeliveryNotAssignableException.class,
        () -> service.create(new CreateAssignmentRequest(delivery, UUID.randomUUID(), null)));
    assertTrue(saved.isEmpty()); verifyNoInteractions(publisher);
    var order = inOrder(manager, cleanup); order.verify(manager).rollback(any()); order.verify(cleanup).compensate(any(), any());
  }

  @Test void localPersistenceFailureCompensatesAfterRollback() {
    doThrow(new IllegalStateException("DB failure")).when(repository).saveAndFlush(any());
    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null)));
    verifyNoInteractions(publisher);
    var order = inOrder(manager, cleanup); order.verify(manager).rollback(any()); order.verify(cleanup).compensate(any(), any());
  }

  @Test void uncertainReserveResponseStillUsesSameTokenForCompensation() {
    var ids = ArgumentCaptor.forClass(UUID.class);
    doThrow(new IllegalStateException("Timed out after Fleet may have committed")).when(fleet).reserve(any(), any());
    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null)));
    verify(fleet).reserve(ids.capture(), any()); verify(cleanup).compensate(eq(ids.getValue()), any());
  }

  @Test void outboxWriteFailureRollsBackAndCompensatesInsteadOfCommittingAnEventlessAssignment() {
    doThrow(new IllegalStateException("Outbox DB failure")).when(publisher).publish(any(DriverAssignedEvent.class));
    assertThrows(IllegalStateException.class,
        () -> service.create(new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null)));
    var order = inOrder(manager, cleanup);
    order.verify(manager).rollback(any()); order.verify(cleanup).compensate(any(), any());
    verify(manager, never()).commit(any());
  }

  @Test void rejectionQueuesCleanupBeforeCommitWithoutCallingFleetInline() {
    var request = new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null);
    var assignment = service.create(request);
    clearInvocations(manager, cleanup, fleet);
    service.rejectAssignment(assignment.id(), request.deliveryId(), "Expired", Instant.now());
    var order = inOrder(cleanup, manager); order.verify(cleanup).enqueue(eq(assignment.id()), any());
    order.verify(manager).commit(any()); verify(fleet, never()).release(any(), any());
  }

  @Test void rejectionAllowsNewAttemptAndLateOldRejectionCannotCloseTheNewOne() {
    var request = new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null);
    var first = service.create(request); service.rejectAssignment(first.id(), request.deliveryId(), "Expired", Instant.now());
    var second = service.create(request); assertNotEquals(first.id(), second.id());
    service.rejectAssignment(first.id(), request.deliveryId(), "Duplicate rejection", Instant.now());
    assertEquals(AssignmentStatus.ACTIVE, service.getByAssignmentId(second.id()).status());
    assertEquals(AssignmentStatus.REJECTED, service.getByAssignmentId(first.id()).status());
  }

  @Test void cancellationRejectionRecordsTerminalKnowledgeAndClosesAssignment() {
    var request = new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null); var assigned = service.create(request);
    service.rejectAssignment(assigned.id(), request.deliveryId(), "Cancelled", "CANCELLED", Instant.now());
    assertEquals(AssignmentStatus.CANCELLED, service.getByAssignmentId(assigned.id()).status());
  }

  @Test void autoAssignmentTriesAnotherCandidateAfterAtomicConflict() {
    var first = UUID.randomUUID(); var second = UUID.randomUUID();
    when(fleet.getAvailableDrivers()).thenReturn(List.of(new DriverResponse(first, "A", "AVAILABLE"), new DriverResponse(second, "B", "AVAILABLE")));
    doThrow(new ReservationConflictException("First candidate lost race")).when(fleet)
        .reserve(any(), argThat((ReservationRequest r) -> r != null && first.equals(r.driverId())));
    var result = service.autoAssign(UUID.randomUUID(), null); assertEquals(second, result.driverId());
  }

  @Test void autoAssignmentRejectsNonPendingAndEmptyCandidates() {
    var delivery = UUID.randomUUID();
    when(deliveries.getDelivery(delivery)).thenReturn(new DeliveryResponse(delivery, customer, null, "P", "D", RemoteDeliveryStatus.CANCELLED));
    assertThrows(DeliveryNotAssignableException.class, () -> service.autoAssign(delivery, null));
    verify(fleet, never()).getAvailableDrivers();
    when(fleet.getAvailableDrivers()).thenReturn(List.of());
    assertThrows(NoAvailableDriverException.class, () -> service.autoAssign(UUID.randomUUID(), null));
  }

  @Test void queriesReturnAssignmentAndMissingAssignmentFails() {
    var request = new CreateAssignmentRequest(UUID.randomUUID(), UUID.randomUUID(), null); var assigned = service.create(request);
    assertEquals(assigned.id(), service.getByDeliveryId(request.deliveryId()).id());
    assertEquals(1, service.getByDriverId(request.driverId()).size());
    assertThrows(AssignmentNotFoundException.class, () -> service.getByAssignmentId(UUID.randomUUID()));
  }

  @Test void overweightOrUnavailableVehicleCannotReachReservation() {
    var delivery=UUID.randomUUID(); var vehicle=UUID.randomUUID();
    when(deliveries.getDelivery(delivery)).thenReturn(new DeliveryResponse(delivery,customer,null,"P","D",RemoteDeliveryStatus.PENDING,new java.math.BigDecimal("100")));
    when(fleet.getVehicles()).thenReturn(List.of(new VehicleResponse(vehicle,"SMALL","VAN",new java.math.BigDecimal("99.99"),true)));
    assertThrows(ReservationConflictException.class,()->service.create(new CreateAssignmentRequest(delivery,UUID.randomUUID(),vehicle)));
    verify(fleet,never()).reserve(any(),any()); verifyNoInteractions(publisher);
  }
  @Test void parcelWeightIsTakenFromDeliveryAndSentToFleet() {
    var delivery=UUID.randomUUID();
    when(deliveries.getDelivery(delivery)).thenReturn(new DeliveryResponse(delivery,customer,null,"P","D",RemoteDeliveryStatus.PENDING,new java.math.BigDecimal("500")));
    service.create(new CreateAssignmentRequest(delivery,UUID.randomUUID(),null));
    verify(fleet).reserve(any(),argThat(r -> r.parcelWeightKg().compareTo(new java.math.BigDecimal("500"))==0 && r.vehicleId()!=null));
  }
  @Test void changingWeightDuringReservationIsCompensatedBeforePublication() {
    var delivery=UUID.randomUUID();
    when(deliveries.getDelivery(delivery)).thenReturn(
      new DeliveryResponse(delivery,customer,null,"P","D",RemoteDeliveryStatus.PENDING,new java.math.BigDecimal("100")),
      new DeliveryResponse(delivery,customer,null,"P","D",RemoteDeliveryStatus.PENDING,new java.math.BigDecimal("101")));
    assertThrows(ReservationConflictException.class,()->service.create(new CreateAssignmentRequest(delivery,UUID.randomUUID(),null)));
    verify(cleanup).compensate(any(),argThat(r -> r.parcelWeightKg().compareTo(new java.math.BigDecimal("100"))==0));
    verifyNoInteractions(publisher);
  }
}
