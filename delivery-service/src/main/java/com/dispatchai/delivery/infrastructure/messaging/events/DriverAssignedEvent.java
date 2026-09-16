package com.dispatchai.delivery.infrastructure.messaging.events;

import java.time.Instant;
import java.util.UUID;

public record DriverAssignedEvent(
    UUID eventId,
    UUID assignmentId,
    UUID deliveryId,
    UUID driverId,
    UUID vehicleId,
    Instant occurredAt) {
}
