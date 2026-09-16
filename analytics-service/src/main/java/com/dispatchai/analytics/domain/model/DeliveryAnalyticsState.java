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
    Instant completedAt) {
}
