package com.dispatchai.delivery.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record DeliveryDelayedEvent(
    UUID eventId,
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    int delayMinutes,
    String reason,
    Instant occurredAt) {
}
