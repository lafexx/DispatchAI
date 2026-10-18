package com.dispatchai.analytics.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAnalyticsState(
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    AnalyticsDeliveryStatus status,
    boolean delayed,
    Integer delayMinutes,
    String delayReason,
    Instant createdAt,
    Instant startedAt,
    Instant completedAt,
    Instant cancelledAt,
    Instant assignedAt,
    Instant lastDelayAt) {

  public static DeliveryAnalyticsState empty() {
    return new DeliveryAnalyticsState(
        null,
        null,
        null,
        null,
        false,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  public DeliveryAnalyticsState apply(
      DeliveryStateUpdate update) {

    var nextStatus = resolveStatus(status, update.status());

    var nextDeliveryId = update.deliveryId() != null
        ? update.deliveryId()
        : deliveryId;

    var nextCustomerId = update.customerId() != null
        ? update.customerId()
        : customerId;

    var nextDriverId = update.driverId() != null
        ? update.driverId()
        : driverId;

    var nextDelayed = delayed || Boolean.TRUE.equals(update.delayed());
    boolean newerDelay = Boolean.TRUE.equals(update.delayed()) && update.eventTime() != null
        && (lastDelayAt == null || update.eventTime().isAfter(lastDelayAt));

    var nextDelayMinutes = newerDelay && update.delayMinutes() != null
        ? update.delayMinutes()
        : delayMinutes;

    var nextDelayReason = newerDelay && update.delayReason() != null
        ? update.delayReason()
        : delayReason;

    var nextCreatedAt = createdAt;
    var nextStartedAt = startedAt;
    var nextCompletedAt = completedAt;
    var nextCancelledAt = cancelledAt;
    var nextAssignedAt = assignedAt;

    if (update.status() == AnalyticsDeliveryStatus.PENDING &&
        nextCreatedAt == null) {
      nextCreatedAt = update.eventTime();
    }

    if (update.status() == AnalyticsDeliveryStatus.ASSIGNED &&
        nextAssignedAt == null) {
      nextAssignedAt = update.eventTime();
    }

    if (update.status() == AnalyticsDeliveryStatus.IN_TRANSIT &&
        nextStartedAt == null) {
      nextStartedAt = update.eventTime();
    }

    if (update.status() == AnalyticsDeliveryStatus.COMPLETED &&
        nextCompletedAt == null) {
      nextCompletedAt = update.eventTime();
    }

    if (update.status() == AnalyticsDeliveryStatus.CANCELLED &&
        nextCancelledAt == null) {
      nextCancelledAt = update.eventTime();
    }

    return new DeliveryAnalyticsState(
        nextDeliveryId,
        nextCustomerId,
        nextDriverId,
        nextStatus,
        nextDelayed,
        nextDelayMinutes,
        nextDelayReason,
        nextCreatedAt,
        nextStartedAt,
        nextCompletedAt,
        nextCancelledAt,
        nextAssignedAt,
        newerDelay ? update.eventTime() : lastDelayAt);
  }

  private static AnalyticsDeliveryStatus resolveStatus(
      AnalyticsDeliveryStatus current,
      AnalyticsDeliveryStatus incoming) {

    if (incoming == null) {
      return current;
    }

    if (current == null) {
      return incoming;
    }

    // Terminal states cannot be downgraded.
    if (current == AnalyticsDeliveryStatus.COMPLETED ||
        current == AnalyticsDeliveryStatus.CANCELLED) {
      return current;
    }

    return switch (incoming) {
      case PENDING -> current;

      case ASSIGNED ->
        current == AnalyticsDeliveryStatus.PENDING
            ? AnalyticsDeliveryStatus.ASSIGNED
            : current;

      case IN_TRANSIT ->
        current == AnalyticsDeliveryStatus.PENDING ||
            current == AnalyticsDeliveryStatus.ASSIGNED
                ? AnalyticsDeliveryStatus.IN_TRANSIT
                : current;

      case COMPLETED ->
        AnalyticsDeliveryStatus.COMPLETED;

      case CANCELLED ->
        AnalyticsDeliveryStatus.CANCELLED;
    };
  }
}
