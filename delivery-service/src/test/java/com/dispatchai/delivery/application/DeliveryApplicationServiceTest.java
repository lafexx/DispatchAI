package com.dispatchai.delivery.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.delivery.domain.model.CancellationReason;
import com.dispatchai.delivery.domain.model.Delivery;
import com.dispatchai.delivery.domain.model.DeliveryStatus;
import com.dispatchai.delivery.domain.exception.DeliveryNotFoundException;
import com.dispatchai.delivery.infrastructure.messaging.publisher.DeliveryEventPublisher;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCreatedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.delivery.infrastructure.persistence.repository.DeliveryJpaRepository;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;

@ExtendWith(MockitoExtension.class)
class DeliveryApplicationServiceTest {

  @Mock
  private DeliveryJpaRepository repository;

  @Mock
  private DeliveryEventPublisher eventPublisher;

  @Mock
  private com.dispatchai.delivery.infrastructure.client.fleet.FleetReservationClient reservations;

  @InjectMocks
  private DeliveryApplicationService service;

  @Test
  void createPersistsDeliveryAndPublishesCreatedEvent() {
    var customerId = UUID.randomUUID();

    var request = new CreateDeliveryRequest(
        customerId,
        "Sydney",
        "Wollongong");

    var result = service.create(request);

    assertEquals(customerId, result.customerId());
    assertEquals("Sydney", result.pickupAddress());
    assertEquals("Wollongong", result.destinationAddress());
    assertEquals(DeliveryStatus.PENDING, result.status());

    verify(repository).save(any(Delivery.class));
    verify(eventPublisher).publish(any(DeliveryCreatedEvent.class));
  }

  @Test
  void createPublishesExpectedDeliveryCreatedEvent() {
    var customerId = UUID.randomUUID();

    var request = new CreateDeliveryRequest(
        customerId,
        "Sydney",
        "Wollongong");

    service.create(request);

    var captor = ArgumentCaptor.forClass(DeliveryCreatedEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertNotNull(event.deliveryId());
    assertEquals(customerId, event.customerId());
    assertEquals("Sydney", event.pickupAddress());
    assertEquals("Wollongong", event.destinationAddress());
    assertNotNull(event.occurredAt());
  }

  @Test
  void getReturnsExistingDelivery() {
    var id = UUID.randomUUID();
    var customerId = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        customerId,
        "Sydney",
        "Wollongong");

    when(repository.findById(id))
        .thenReturn(Optional.of(delivery));

    var result = service.get(id);

    assertEquals(id, result.id());
    assertEquals(customerId, result.customerId());
    assertEquals(DeliveryStatus.PENDING, result.status());
  }

  @Test
  void getThrowsWhenDeliveryDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findById(id))
        .thenReturn(Optional.empty());

    assertThrows(
        DeliveryNotFoundException.class,
        () -> service.get(id));
  }

  @Test
  void assignLoadsDeliveryAndPersistsUpdatedState() {
    var id = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        UUID.randomUUID(),
        "Sydney",
        "Wollongong");

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    var result = service.assign(id, driverId);

    assertEquals(DeliveryStatus.ASSIGNED, result.status());
    assertEquals(driverId, result.assignedDriverId());

    verify(repository).save(delivery);
  }

  @Test
  void assignDoesNotPublishDeliveryLifecycleEvent() {
    var id = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        UUID.randomUUID(),
        "Sydney",
        "Wollongong");

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    service.assign(id, UUID.randomUUID());

    verify(eventPublisher, never())
        .publish(any(DeliveryStartedEvent.class));
    verify(eventPublisher, never())
        .publish(any(DeliveryCompletedEvent.class));
    verify(eventPublisher, never())
        .publish(any(DeliveryCancelledEvent.class));
    verify(eventPublisher, never())
        .publish(any(DeliveryDelayedEvent.class));
  }

  @Test
  void startLoadsDeliveryPersistsUpdatedStateAndPublishesEvent() {
    var id = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        UUID.randomUUID(),
        "Sydney",
        "Wollongong");

    delivery.assign(UUID.randomUUID());

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    var result = service.start(id);

    assertEquals(
        DeliveryStatus.IN_TRANSIT,
        result.status());

    verify(repository).save(delivery);

    verify(eventPublisher)
        .publish(any(DeliveryStartedEvent.class));
  }

  @Test
  void startPublishesDeliveryStartedEvent() {
    var id = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        customerId,
        "Sydney",
        "Wollongong");

    delivery.assign(driverId);

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    service.start(id);

    var captor = ArgumentCaptor.forClass(DeliveryStartedEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertEquals(id, event.deliveryId());
    assertEquals(customerId, event.customerId());
    assertEquals(driverId, event.driverId());
    assertNotNull(event.occurredAt());
  }

  @Test
  void completePersistsCompletedStateAndPublishesEvent() {
    var id = UUID.randomUUID();

    var delivery = createInTransitDelivery(id);

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    var result = service.complete(id);

    assertEquals(
        DeliveryStatus.COMPLETED,
        result.status());

    verify(repository).save(delivery);
    verify(eventPublisher)
        .publish(any(DeliveryCompletedEvent.class));
  }

  @Test
  void completePublishesExpectedDeliveryCompletedEvent() {
    var id = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        customerId,
        "Sydney",
        "Wollongong");

    delivery.assign(driverId);
    delivery.start();

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    service.complete(id);

    var captor = ArgumentCaptor.forClass(DeliveryCompletedEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertEquals(id, event.deliveryId());
    assertEquals(customerId, event.customerId());
    assertEquals(driverId, event.driverId());
    assertNotNull(event.occurredAt());
  }

  @Test
  void cancelPersistsCancelledStateAndPublishesEvent() {
    var id = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        UUID.randomUUID(),
        "Sydney",
        "Wollongong");

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    var request = new CancelDeliveryRequest(
        CancellationReason.CUSTOMER_REQUEST,
        "Customer requested cancellation");

    var result = service.cancel(id, request);

    assertEquals(
        DeliveryStatus.CANCELLED,
        result.status());

    verify(repository).save(delivery);
    verify(eventPublisher)
        .publish(any(DeliveryCancelledEvent.class));
  }

  @Test
  void cancelPublishesExpectedDeliveryCancelledEvent() {
    var id = UUID.randomUUID();
    var customerId = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        customerId,
        "Sydney",
        "Wollongong");

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    var request = new CancelDeliveryRequest(
        CancellationReason.CUSTOMER_REQUEST,
        "Customer requested cancellation");

    service.cancel(id, request);

    var captor = ArgumentCaptor.forClass(DeliveryCancelledEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertEquals(id, event.deliveryId());
    assertEquals(customerId, event.customerId());
    assertEquals(CancellationReason.CUSTOMER_REQUEST, event.reason());
    assertEquals(
        "Customer requested cancellation",
        event.notes());
    assertNotNull(event.occurredAt());
  }

  @Test
  void delayPersistsDelayStateAndPublishesEvent() {
    var id = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        UUID.randomUUID(),
        "Sydney",
        "Wollongong");

    delivery.assign(UUID.randomUUID());

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    var result = service.delay(
        id,
        30,
        "Heavy traffic");

    assertTrue(result.delayed());
    assertEquals(30, result.delayMinutes());
    assertEquals("Heavy traffic", result.delayReason());

    verify(repository).save(delivery);
    verify(eventPublisher)
        .publish(any(DeliveryDelayedEvent.class));
  }

  @Test
  void delayPublishesExpectedDeliveryDelayedEvent() {
    var id = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var driverId = UUID.randomUUID();

    var delivery = new Delivery(
        id,
        customerId,
        "Sydney",
        "Wollongong");

    delivery.assign(driverId);

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.of(delivery));

    service.delay(
        id,
        30,
        "Heavy traffic");

    var captor = ArgumentCaptor.forClass(DeliveryDelayedEvent.class);

    verify(eventPublisher).publish(captor.capture());

    var event = captor.getValue();

    assertNotNull(event.eventId());
    assertEquals(id, event.deliveryId());
    assertEquals(customerId, event.customerId());
    assertEquals(driverId, event.driverId());
    assertEquals(30, event.delayMinutes());
    assertEquals("Heavy traffic", event.reason());
    assertNotNull(event.occurredAt());
  }

  @Test
  void operationThrowsWhenDeliveryDoesNotExist() {
    var id = UUID.randomUUID();

    when(repository.findByIdForUpdate(id))
        .thenReturn(Optional.empty());

    assertThrows(
        DeliveryNotFoundException.class,
        () -> service.start(id));

    verify(repository, never())
        .save(any());

    verify(eventPublisher, never())
        .publish(any(DeliveryStartedEvent.class));
  }

  private Delivery createInTransitDelivery(UUID id) {
    var delivery = new Delivery(
        id,
        UUID.randomUUID(),
        "Sydney",
        "Wollongong");

    delivery.assign(UUID.randomUUID());
    delivery.start();

    return delivery;
  }
}
