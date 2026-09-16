package com.dispatchai.delivery.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record DeliveryCreatedEvent(
    UUID eventId,
    UUID deliveryId,
    UUID customerId,
    String pickupAddress,
    String destinationAddress,
    Instant occurredAt) {
}
