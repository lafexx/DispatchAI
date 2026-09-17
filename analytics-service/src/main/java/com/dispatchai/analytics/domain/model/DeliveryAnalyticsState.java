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
    Instant assignedAt) {

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
        null);
  }

  public DeliveryAnalyticsState apply(DeliveryStateUpdate update) {
    return new DeliveryAnalyticsState(
        update.deliveryId() != null
            ? update.deliveryId()
            : deliveryId,

        update.customerId() != null
            ? update.customerId()
            : customerId,

        update.driverId() != null
            ? update.driverId()
            : driverId,

        update.status() != null
            ? update.status()
            : status,

        update.delayed() != null
            ? update.delayed()
            : delayed,

        update.delayMinutes() != null
            ? update.delayMinutes()
            : delayMinutes,

        update.delayReason() != null
            ? update.delayReason()
            : delayReason,

        update.status() == AnalyticsDeliveryStatus.PENDING
            ? update.eventTime()
            : createdAt,

        update.status() == AnalyticsDeliveryStatus.IN_TRANSIT
            ? update.eventTime()
            : startedAt,

        update.status() == AnalyticsDeliveryStatus.COMPLETED
            ? update.eventTime()
            : completedAt,

        update.status() == AnalyticsDeliveryStatus.CANCELLED
            ? update.eventTime()
            : cancelledAt,

        update.status() == AnalyticsDeliveryStatus.ASSIGNED
            ? update.eventTime()
            : assignedAt);
  }
}
