package com.dispatchai.delivery.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

import com.dispatchai.delivery.domain.model.CancellationReason;

public record DeliveryCancelledEvent(
    UUID eventId,
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    CancellationReason reason,
    String notes,
    Instant occurredAt) {
}
