package com.dispatchai.delivery.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record DeliveryStartedEvent(
    UUID eventId,
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    Instant occurredAt) {
}
