package com.dispatchai.dispatch.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.exceptions.AssignmentNotFoundException;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotAssignableException;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotFoundException;
import com.dispatchai.dispatch.domain.model.exceptions.NoAvailableDriverException;
import com.dispatchai.dispatch.infrastructure.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.delivery.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.fleet.FleetDriverResponse;
import com.dispatchai.dispatch.infrastructure.messaging.AssignmentEventPublisher;
import com.dispatchai.dispatch.infrastructure.messaging.DriverAssignedEvent;
import com.dispatchai.dispatch.infrastructure.persistence.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@ExtendWith(MockitoExtension.class)
class DispatchApplicationServiceTest {

  @Mock
  private FleetClient fleetClient;

  @Mock
  private DeliveryClient deliveryClient;

  @Mock
  private DeliveryAssignmentJpaRepository repository;

  @Mock
  private AssignmentEventPublisher eventPublisher;

  @InjectMocks
  private DispatchApplicationService service;

  @Test
  void createPersistsAssignmentAndPublishesEvent() {
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();
    var vehicleId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getDriver(driverId))
        .thenReturn(new FleetDriverResponse(
            driverId,
            "Alex Smith",
            "AVAILABLE"));

    var request = new CreateAssignmentRequest(
        deliveryId,
        driverId,
        vehicleId);

    var result = service.create(request);

    assertEquals(deliveryId, result.deliveryId());
    assertEquals(driverId, result.driverId());
    assertEquals(vehicleId, result.vehicleId());
    assertEquals(AssignmentStatus.ACTIVE, result.status());

    verify(repository)
        .save(any(DeliveryAssignment.class));

    verify(eventPublisher)
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void createPublishesExpectedDriverAssignedEvent() {
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();
    var vehicleId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getDriver(driverId))
        .thenReturn(new FleetDriverResponse(
            driverId,
            "Alex Smith",
            "AVAILABLE"));

    service.create(
        new CreateAssignmentRequest(
            deliveryId,
            driverId,
            vehicleId));

    var captor = ArgumentCaptor.forClass(DriverAssignedEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertNotNull(event.assignmentId());
    assertEquals(deliveryId, event.deliveryId());
    assertEquals(driverId, event.driverId());
    assertEquals(vehicleId, event.vehicleId());
    assertNotNull(event.occurredAt());
  }

  @Test
  void createRejectsNonPendingDelivery() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(
            new DeliveryResponse(
                deliveryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sydney",
                "Wollongong",
                RemoteDeliveryStatus.ASSIGNED));

    var request = new CreateAssignmentRequest(
        deliveryId,
        UUID.randomUUID(),
        null);

    assertThrows(
        DeliveryNotAssignableException.class,
        () -> service.create(request));

    verify(fleetClient, never())
        .getDriver(any());

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void createRejectsDeliveryWithExistingActiveAssignment() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(true);

    var request = new CreateAssignmentRequest(
        deliveryId,
        UUID.randomUUID(),
        null);

    assertThrows(
        IllegalStateException.class,
        () -> service.create(request));

    verify(fleetClient, never())
        .getDriver(any());

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void createRejectsUnavailableDriver() {
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getDriver(driverId))
        .thenReturn(new FleetDriverResponse(
            driverId,
            "Alex Smith",
            "UNAVAILABLE"));

    var request = new CreateAssignmentRequest(
        deliveryId,
        driverId,
        null);

    assertThrows(
        IllegalStateException.class,
        () -> service.create(request));

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void autoAssignUsesFirstAvailableDriver() {
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getAvailableDrivers())
        .thenReturn(List.of(
            new FleetDriverResponse(
                driverId,
                "Alex Smith",
                "AVAILABLE")));

    var result = service.autoAssign(
        deliveryId,
        null);

    assertEquals(deliveryId, result.deliveryId());
    assertEquals(driverId, result.driverId());
    assertEquals(AssignmentStatus.ACTIVE, result.status());

    verify(repository)
        .save(any(DeliveryAssignment.class));

    verify(eventPublisher)
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void autoAssignPublishesExpectedDriverAssignedEvent() {
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();
    var vehicleId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getAvailableDrivers())
        .thenReturn(List.of(
            new FleetDriverResponse(
                driverId,
                "Alex Smith",
                "AVAILABLE")));

    service.autoAssign(
        deliveryId,
        vehicleId);

    var captor = ArgumentCaptor.forClass(DriverAssignedEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertNotNull(event.assignmentId());
    assertEquals(deliveryId, event.deliveryId());
    assertEquals(driverId, event.driverId());
    assertEquals(vehicleId, event.vehicleId());
    assertNotNull(event.occurredAt());
  }

  @Test
  void autoAssignRejectsNonPendingDelivery() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(
            new DeliveryResponse(
                deliveryId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Sydney",
                "Wollongong",
                RemoteDeliveryStatus.ASSIGNED));

    assertThrows(
        DeliveryNotAssignableException.class,
        () -> service.autoAssign(
            deliveryId,
            null));

    verify(fleetClient, never())
        .getAvailableDrivers();

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void autoAssignRejectsDeliveryWithExistingActiveAssignment() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(true);

    assertThrows(
        IllegalStateException.class,
        () -> service.autoAssign(
            deliveryId,
            null));

    verify(fleetClient, never())
        .getAvailableDrivers();

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void autoAssignRejectsWhenAvailableDriversIsEmpty() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getAvailableDrivers())
        .thenReturn(List.of());

    assertThrows(
        NoAvailableDriverException.class,
        () -> service.autoAssign(
            deliveryId,
            null));

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void autoAssignRejectsWhenAvailableDriversIsNull() {
    var deliveryId = UUID.randomUUID();

    when(deliveryClient.getDelivery(deliveryId))
        .thenReturn(pendingDelivery(deliveryId));

    when(repository.existsByDeliveryIdAndStatus(
        deliveryId,
        AssignmentStatus.ACTIVE))
        .thenReturn(false);

    when(fleetClient.getAvailableDrivers())
        .thenReturn(null);

    assertThrows(
        NoAvailableDriverException.class,
        () -> service.autoAssign(
            deliveryId,
            null));

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DriverAssignedEvent.class));
  }

  @Test
  void getByAssignmentIdReturnsAssignment() {
    var assignmentId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    var assignment = new DeliveryAssignment(
        assignmentId,
        deliveryId,
        driverId,
        null);

    when(repository.findById(assignmentId))
        .thenReturn(Optional.of(assignment));

    var result = service.getByAssignmentId(assignmentId);

    assertEquals(assignmentId, result.id());
    assertEquals(deliveryId, result.deliveryId());
    assertEquals(driverId, result.driverId());
    assertEquals(AssignmentStatus.ACTIVE, result.status());
  }

  @Test
  void getByAssignmentIdThrowsWhenAssignmentDoesNotExist() {
    var assignmentId = UUID.randomUUID();

    when(repository.findById(assignmentId))
        .thenReturn(Optional.empty());

    assertThrows(
        AssignmentNotFoundException.class,
        () -> service.getByAssignmentId(
            assignmentId));
  }

  @Test
  void getByDeliveryIdReturnsAssignment() {
    var assignmentId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    var assignment = new DeliveryAssignment(
        assignmentId,
        deliveryId,
        driverId,
        null);

    when(repository.findByDeliveryId(deliveryId))
        .thenReturn(Optional.of(assignment));

    var result = service.getByDeliveryId(deliveryId);

    assertEquals(assignmentId, result.id());
    assertEquals(deliveryId, result.deliveryId());
    assertEquals(driverId, result.driverId());
  }

  @Test
  void getByDeliveryIdThrowsWhenAssignmentDoesNotExist() {
    var deliveryId = UUID.randomUUID();

    when(repository.findByDeliveryId(deliveryId))
        .thenReturn(Optional.empty());

    assertThrows(
        DeliveryNotFoundException.class,
        () -> service.getByDeliveryId(
            deliveryId));
  }

  private DeliveryResponse pendingDelivery(
      UUID deliveryId) {

    return new DeliveryResponse(
        deliveryId,
        UUID.randomUUID(),
        null,
        "Sydney",
        "Wollongong",
        RemoteDeliveryStatus.PENDING);
  }
}
