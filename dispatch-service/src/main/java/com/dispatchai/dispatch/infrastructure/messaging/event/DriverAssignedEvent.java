package com.dispatchai.dispatch.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record DriverAssignedEvent(
    UUID eventId,
    UUID assignmentId,
    UUID deliveryId,
    UUID driverId,
    UUID customerId,
    UUID vehicleId,
    Instant occurredAt) {
}
