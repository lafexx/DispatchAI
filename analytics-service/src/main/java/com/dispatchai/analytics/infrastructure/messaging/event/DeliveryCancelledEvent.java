package com.dispatchai.analytics.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCancelledEvent(
    UUID eventId,
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    String reason,
    String notes,
    Instant occurredAt) {
}
