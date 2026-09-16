package com.dispatchai.analytics.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCompletedEvent(
    UUID eventId,
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    Instant occurredAt) {
}
