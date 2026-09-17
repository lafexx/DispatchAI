package com.dispatchai.analytics.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DeliveryStateUpdate(
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    AnalyticsDeliveryStatus status,
    Boolean delayed,
    Integer delayMinutes,
    String delayReason,
    Instant eventTime) {
}
