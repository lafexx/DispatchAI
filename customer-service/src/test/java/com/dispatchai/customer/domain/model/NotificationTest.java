package com.dispatchai.customer.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class NotificationTest {

  @Test
  void notificationIsCreatedWithExpectedValues() {
    UUID id = UUID.randomUUID();
    UUID sourceEventId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID deliveryId = UUID.randomUUID();

    Instant beforeCreation = Instant.now();

    Notification notification = new Notification(
        id,
        sourceEventId,
        customerId,
        deliveryId,
        NotificationType.DELIVERY_STARTED,
        "Your delivery is now in transit.");

    Instant afterCreation = Instant.now();

    assertEquals(id, notification.getId());
    assertEquals(sourceEventId, notification.getSourceEventId());
    assertEquals(customerId, notification.getCustomerId());
    assertEquals(deliveryId, notification.getDeliveryId());
    assertEquals(NotificationType.DELIVERY_STARTED, notification.getType());
    assertEquals(
        "Your delivery is now in transit.",
        notification.getMessage());

    assertEquals(NotificationStatus.PENDING, notification.getStatus());
    assertNotNull(notification.getCreatedAt());

    assertTrue(
        !notification.getCreatedAt().isBefore(beforeCreation)
            && !notification.getCreatedAt().isAfter(afterCreation));
  }

  @Test
  void newNotificationStartsPending() {
    Notification notification = createNotification();

    assertEquals(NotificationStatus.PENDING, notification.getStatus());
  }

  @Test
  void notificationIdCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            NotificationType.DELIVERY_STARTED,
            "Test message"));

    assertEquals(
        "Notification ID cannot be null",
        exception.getMessage());
  }

  @Test
  void sourceEventIdCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            UUID.randomUUID(),
            NotificationType.DELIVERY_STARTED,
            "Test message"));

    assertEquals(
        "Source event ID cannot be null",
        exception.getMessage());
  }

  @Test
  void customerIdCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            NotificationType.DELIVERY_STARTED,
            "Test message"));

    assertEquals(
        "Customer ID cannot be null",
        exception.getMessage());
  }

  @Test
  void deliveryIdCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            NotificationType.DELIVERY_STARTED,
            "Test message"));

    assertEquals(
        "Delivery ID cannot be null",
        exception.getMessage());
  }

  @Test
  void notificationTypeCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            "Test message"));

    assertEquals(
        "Notification type cannot be null",
        exception.getMessage());
  }

  @Test
  void notificationMessageCannotBeBlank() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Notification(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            NotificationType.DELIVERY_STARTED,
            "   "));

    assertEquals(
        "Notification message cannot be blank",
        exception.getMessage());
  }

  @Test
  void newNotificationHasNoSentAtTimestamp() {
    Notification notification = createNotification();

    assertNull(notification.getSentAt());
  }

  @Test
  void notificationCanBeMarkedSent() {
    Notification notification = createNotification();

    Instant beforeSent = Instant.now();

    notification.markSent();

    Instant afterSent = Instant.now();

    assertEquals(NotificationStatus.SENT, notification.getStatus());
    assertNotNull(notification.getSentAt());

    assertTrue(
        !notification.getSentAt().isBefore(beforeSent)
            && !notification.getSentAt().isAfter(afterSent));
  }

  private Notification createNotification() {
    return new Notification(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        NotificationType.DELIVERY_STARTED,
        "Your delivery is now in transit.");
  }
}
