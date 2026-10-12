package com.dispatchai.customer.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dispatchai.customer.domain.exceptions.CustomerNotFoundException;
import com.dispatchai.customer.domain.model.Customer;
import com.dispatchai.customer.domain.model.Notification;
import com.dispatchai.customer.domain.model.NotificationStatus;
import com.dispatchai.customer.domain.model.NotificationType;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DriverAssignedEvent;
import com.dispatchai.customer.infrastructure.persistence.CustomerJpaRepository;
import com.dispatchai.customer.infrastructure.persistence.NotificationJpaRepository;

@ExtendWith(MockitoExtension.class)
class NotificationApplicationServiceTest {

  @Mock
  private CustomerJpaRepository customerRepository;

  @Mock
  private NotificationJpaRepository notificationRepository;

  @InjectMocks
  private NotificationApplicationService service;

  @Test
  void getForCustomerReturnsMappedNotifications() {
    var customerId = UUID.randomUUID();

    var first = new Notification(
        UUID.randomUUID(),
        UUID.randomUUID(),
        customerId,
        UUID.randomUUID(),
        NotificationType.DELIVERY_STARTED,
        "Your delivery is now in transit.");

    var second = new Notification(
        UUID.randomUUID(),
        UUID.randomUUID(),
        customerId,
        UUID.randomUUID(),
        NotificationType.DELIVERY_COMPLETED,
        "Your delivery has been completed.");

    first.markSent();
    second.markSent();

    when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
        .thenReturn(List.of(first, second));

    var result = service.getForCustomer(customerId);

    assertEquals(2, result.size());

    assertEquals(first.getId(), result.get(0).id());
    assertEquals(first.getSourceEventId(), result.get(0).sourceEventId());
    assertEquals(first.getCustomerId(), result.get(0).customerId());
    assertEquals(first.getDeliveryId(), result.get(0).deliveryId());
    assertEquals(first.getType(), result.get(0).type());
    assertEquals(first.getMessage(), result.get(0).message());
    assertEquals(NotificationStatus.SENT, result.get(0).status());

    assertEquals(second.getId(), result.get(1).id());
    assertEquals(NotificationType.DELIVERY_COMPLETED, result.get(1).type());
    assertEquals(NotificationStatus.SENT, result.get(1).status());
  }

  @Test
  void getForCustomerReturnsEmptyListWhenNoNotificationsExist() {
    var customerId = UUID.randomUUID();

    when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId))
        .thenReturn(List.of());

    var result = service.getForCustomer(customerId);

    assertEquals(0, result.size());
  }

  @Test
  void notifyDriverAssignedCreatesExpectedNotification() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();

    when(notificationRepository.existsBySourceEventId(eventId))
        .thenReturn(false);

    when(customerRepository.findById(customerId))
        .thenReturn(Optional.of(customer(customerId)));

    var event = new DriverAssignedEvent(
        eventId,
        UUID.randomUUID(),
        deliveryId,
        UUID.randomUUID(),
        customerId,
        null,
        Instant.now());

    service.notifyDriverAssigned(event);

    var captor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationRepository, times(2))
        .save(captor.capture());

    var notification = captor.getAllValues().getFirst();

    assertEquals(eventId, notification.getSourceEventId());
    assertEquals(customerId, notification.getCustomerId());
    assertEquals(deliveryId, notification.getDeliveryId());
    assertEquals(NotificationType.DRIVER_ASSIGNED, notification.getType());
    assertEquals(
        "A driver has been assigned to your delivery.",
        notification.getMessage());

    assertEquals(
        NotificationStatus.SENT,
        captor.getAllValues().getLast().getStatus());
  }

  @Test
  void notifyDeliveryStartedCreatesExpectedNotification() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();

    prepareCustomer(eventId, customerId);

    var event = new DeliveryStartedEvent(
        eventId,
        deliveryId,
        customerId,
        UUID.randomUUID(),
        Instant.now());

    service.notifyDeliveryStarted(event);

    var notification = captureSavedNotification();

    assertEquals(NotificationType.DELIVERY_STARTED, notification.getType());
    assertEquals(
        "Your delivery is now in transit.",
        notification.getMessage());
    assertEquals(deliveryId, notification.getDeliveryId());
    assertEquals(customerId, notification.getCustomerId());
  }

  @Test
  void notifyDeliveryCompletedCreatesExpectedNotification() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();

    prepareCustomer(eventId, customerId);

    var event = new DeliveryCompletedEvent(
        eventId,
        deliveryId,
        customerId,
        UUID.randomUUID(),
        Instant.now());

    service.notifyDeliveryCompleted(event);

    var notification = captureSavedNotification();

    assertEquals(NotificationType.DELIVERY_COMPLETED, notification.getType());
    assertEquals(
        "Your delivery has been completed.",
        notification.getMessage());
  }

  @Test
  void notifyDeliveryCancelledCreatesExpectedNotification() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();

    prepareCustomer(eventId, customerId);

    var event = new DeliveryCancelledEvent(
        eventId,
        deliveryId,
        customerId,
        UUID.randomUUID(),
        "CUSTOMER_REQUEST",
        "Customer requested cancellation",
        Instant.now());

    service.notifyDeliveryCancelled(event);

    var notification = captureSavedNotification();

    assertEquals(NotificationType.DELIVERY_CANCELLED, notification.getType());
    assertEquals(
        "Your delivery has been cancelled: CUSTOMER_REQUEST",
        notification.getMessage());
  }

  @Test
  void notifyDeliveryDelayedCreatesExpectedNotification() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();
    var deliveryId = UUID.randomUUID();

    prepareCustomer(eventId, customerId);

    var event = new DeliveryDelayedEvent(
        eventId,
        deliveryId,
        customerId,
        UUID.randomUUID(),
        30,
        "Heavy traffic",
        Instant.now());

    service.notifyDeliveryDelayed(event);

    var notification = captureSavedNotification();

    assertEquals(NotificationType.DELIVERY_DELAYED, notification.getType());
    assertEquals(
        "Your delivery has been delayed by 30 minutes. Reason: Heavy traffic",
        notification.getMessage());
  }

  @Test
  void duplicateEventDoesNotCreateNotification() {
    var eventId = UUID.randomUUID();

    when(notificationRepository.existsBySourceEventId(eventId))
        .thenReturn(true);

    var event = new DeliveryStartedEvent(
        eventId,
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        Instant.now());

    service.notifyDeliveryStarted(event);

    verify(customerRepository, never())
        .findById(any());

    verify(notificationRepository, never())
        .save(any());
  }

  @Test
  void missingCustomerThrowsAndDoesNotCreateNotification() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();

    when(notificationRepository.existsBySourceEventId(eventId))
        .thenReturn(false);

    when(customerRepository.findById(customerId))
        .thenReturn(Optional.empty());

    var event = new DeliveryCompletedEvent(
        eventId,
        UUID.randomUUID(),
        customerId,
        UUID.randomUUID(),
        Instant.now());

    assertThrows(
        CustomerNotFoundException.class,
        () -> service.notifyDeliveryCompleted(event));

    verify(notificationRepository, never())
        .save(any());
  }

  @Test
  void newlyCreatedNotificationIsMarkedSent() {
    var eventId = UUID.randomUUID();
    var customerId = UUID.randomUUID();

    prepareCustomer(eventId, customerId);

    var event = new DeliveryStartedEvent(
        eventId,
        UUID.randomUUID(),
        customerId,
        UUID.randomUUID(),
        Instant.now());

    service.notifyDeliveryStarted(event);

    var captor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationRepository, times(2))
        .save(captor.capture());

    var finalNotification = captor.getAllValues().getLast();

    assertEquals(
        NotificationStatus.SENT,
        finalNotification.getStatus());
  }

  private void prepareCustomer(
      UUID eventId,
      UUID customerId) {

    when(notificationRepository.existsBySourceEventId(eventId))
        .thenReturn(false);

    when(customerRepository.findById(customerId))
        .thenReturn(Optional.of(customer(customerId)));
  }

  private Customer customer(UUID id) {
    return new Customer(
        id,
        "Alex Smith",
        "alex@example.com",
        "0400000000");
  }

  private Notification captureSavedNotification() {
    var captor = ArgumentCaptor.forClass(Notification.class);

    verify(notificationRepository, times(2))
        .save(captor.capture());

    return captor.getAllValues().getFirst();
  }
}
