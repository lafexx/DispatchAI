package com.dispatchai.analytics.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DriverAssignedEvent(
    UUID eventId,
    UUID assignmentId,
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    UUID vehicleId,
    Instant occurredAt) {
}
