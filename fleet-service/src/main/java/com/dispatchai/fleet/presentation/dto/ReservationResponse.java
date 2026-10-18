package com.dispatchai.fleet.presentation.dto;

import java.time.Instant;
import java.util.UUID;
import com.dispatchai.fleet.domain.model.FleetReservationStatus;

public record ReservationResponse(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId,
    FleetReservationStatus status, Instant expiresAt, String releaseReason) {
}
