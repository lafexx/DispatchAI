package com.dispatchai.delivery.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAssignedEvent(UUID eventId, UUID assignmentId, UUID deliveryId, UUID driverId, UUID customerId,
    UUID vehicleId, Instant occurredAt) {
}
