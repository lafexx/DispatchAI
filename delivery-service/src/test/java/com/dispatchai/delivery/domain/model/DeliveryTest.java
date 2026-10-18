package com.dispatchai.delivery.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dispatchai.delivery.domain.exception.InvalidDeliveryStateException;

class DeliveryTest {
  private Delivery createDelivery() {
    return new Delivery(UUID.randomUUID(), UUID.randomUUID(), "1 Bing Street, Sydney", "2 Bang Street, Wollongong");
  }

  @Test
  void newDeliveryStartsPending() {
    var delivery = createDelivery();

    assertEquals(DeliveryStatus.PENDING, delivery.getStatus());
  }

  @Test
  void pendingDeliveryCanBeAssigned() {
    var delivery = createDelivery();
    var driverId = UUID.randomUUID();

    delivery.assign(driverId);

    assertEquals(DeliveryStatus.ASSIGNED, delivery.getStatus());

    assertEquals(driverId, delivery.getAssignedDriverId());
  }

  @Test
  void assignedDeliveryCanBeStarted() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());
    delivery.start();

    assertEquals(DeliveryStatus.IN_TRANSIT, delivery.getStatus());
  }

  @Test
  void inTransitDeliveryCanBeCompleted() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());
    delivery.start();
    delivery.complete();

    assertEquals(
        DeliveryStatus.COMPLETED,
        delivery.getStatus());
  }

  @Test
  void pendingDeliveryCannotBeStarted() {
    var delivery = createDelivery();

    assertThrows(
        InvalidDeliveryStateException.class,
        delivery::start);

    assertEquals(
        DeliveryStatus.PENDING,
        delivery.getStatus());
  }

  @Test
  void pendingDeliveryCannotBeCompleted() {
    var delivery = createDelivery();

    assertThrows(
        InvalidDeliveryStateException.class,
        delivery::complete);

    assertEquals(
        DeliveryStatus.PENDING,
        delivery.getStatus());
  }

  @Test
  void assignedDeliveryCannotBeCompletedBeforeStarting() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());

    assertThrows(
        InvalidDeliveryStateException.class,
        delivery::complete);

    assertEquals(
        DeliveryStatus.ASSIGNED,
        delivery.getStatus());
  }

  @Test
  void pendingDeliveryCanBeCancelled() {
    var delivery = createDelivery();

    delivery.cancel(CancellationReason.CUSTOMER_REQUEST, "Cancellation requested by customer");

    assertEquals(
        DeliveryStatus.CANCELLED,
        delivery.getStatus());
  }

  @Test
  void assignedDeliveryCanBeCancelled() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());
    delivery.cancel(CancellationReason.CUSTOMER_REQUEST, "Cancellation requested by customer");

    assertEquals(
        DeliveryStatus.CANCELLED,
        delivery.getStatus());
  }

  @Test
  void completedDeliveryCannotBeCancelled() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());
    delivery.start();
    delivery.complete();

    assertThrows(
        InvalidDeliveryStateException.class,
        () -> delivery.cancel(CancellationReason.CUSTOMER_REQUEST, "Cancellation requested by customer"));

    assertEquals(
        DeliveryStatus.COMPLETED,
        delivery.getStatus());
  }

  @Test
  void cancelledDeliveryCannotBeStarted() {
    var delivery = createDelivery();

    delivery.cancel(CancellationReason.CUSTOMER_REQUEST, "Cancellation requested by customer");

    assertThrows(
        InvalidDeliveryStateException.class,
        delivery::start);

    assertEquals(
        DeliveryStatus.CANCELLED,
        delivery.getStatus());
  }

  @Test
  void deliveryCannotBeAssignedToNullDriver() {
    var delivery = createDelivery();

    assertThrows(
        IllegalArgumentException.class,
        () -> delivery.assign(null));

    assertEquals(
        DeliveryStatus.PENDING,
        delivery.getStatus());

    assertNull(delivery.getAssignedDriverId());
  }

  @Test
  void assignedDeliveryCannotBeAssignedAgain() {
    var delivery = createDelivery();

    var firstDriver = UUID.randomUUID();

    delivery.assign(firstDriver);

    assertThrows(
        InvalidDeliveryStateException.class,
        () -> delivery.assign(UUID.randomUUID()));

    assertEquals(
        firstDriver,
        delivery.getAssignedDriverId());
  }

  @Test
  void activeDeliveryCanBeMarkedDelayed() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());
    delivery.start();

    delivery.markDelayed(30, "Heavy traffic");

    assertEquals(true, delivery.isDelayed());
    assertEquals(30, delivery.getDelayMinutes());
    assertEquals("Heavy traffic", delivery.getDelayReason());
  }

  @Test
  void delayMinutesMustBePositive() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());

    assertThrows(
        IllegalArgumentException.class,
        () -> delivery.markDelayed(0, "Heavy traffic"));

    assertEquals(false, delivery.isDelayed());
  }

  @Test
  void delayReasonCannotBeBlank() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());

    assertThrows(
        IllegalArgumentException.class,
        () -> delivery.markDelayed(30, "   "));

    assertEquals(false, delivery.isDelayed());
  }

  @Test
  void completedDeliveryCannotBeDelayed() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());
    delivery.start();
    delivery.complete();

    assertThrows(
        InvalidDeliveryStateException.class,
        () -> delivery.markDelayed(30, "Heavy traffic"));

    assertEquals(
        DeliveryStatus.COMPLETED,
        delivery.getStatus());

    assertEquals(false, delivery.isDelayed());
  }

  @Test
  void cancelledDeliveryCannotBeDelayed() {
    var delivery = createDelivery();

    delivery.cancel(
        CancellationReason.CUSTOMER_REQUEST,
        "Cancellation requested by customer");

    assertThrows(
        InvalidDeliveryStateException.class,
        () -> delivery.markDelayed(30, "Heavy traffic"));

    assertEquals(
        DeliveryStatus.CANCELLED,
        delivery.getStatus());

    assertEquals(false, delivery.isDelayed());
  }

  @Test
  void markingDelayedDoesNotChangeLifecycleStatus() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());

    delivery.markDelayed(30, "Heavy traffic");

    assertEquals(
        DeliveryStatus.ASSIGNED,
        delivery.getStatus());

    assertEquals(true, delivery.isDelayed());
  }

  @Test
  void markingDeliveryDelayedSetsDelayedAtTimestamp() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());

    var before = java.time.Instant.now();

    delivery.markDelayed(30, "Heavy traffic");

    var after = java.time.Instant.now();

    assertEquals(true, delivery.isDelayed());

    var delayedAt = delivery.getDelayedAt();

    assertEquals(false, delayedAt.isBefore(before));
    assertEquals(false, delayedAt.isAfter(after));
  }

  @Test
  void invalidDelayDoesNotMutateDelivery() {
    var delivery = createDelivery();

    delivery.assign(UUID.randomUUID());

    assertThrows(
        IllegalArgumentException.class,
        () -> delivery.markDelayed(-10, "Heavy traffic"));

    assertFalse(delivery.isDelayed());
    assertNull(delivery.getDelayMinutes());
    assertNull(delivery.getDelayReason());
    assertNull(delivery.getDelayedAt());
  }
}
