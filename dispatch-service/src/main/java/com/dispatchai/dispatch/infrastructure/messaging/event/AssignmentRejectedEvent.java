
package com.dispatchai.dispatch.infrastructure.messaging.event;

import java.time.Instant;
import java.util.UUID;

public record AssignmentRejectedEvent(UUID eventId, UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId,
    String reason, String deliveryStatus, Instant occurredAt) {
}
