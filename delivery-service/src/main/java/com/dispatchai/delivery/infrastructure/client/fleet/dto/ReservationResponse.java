package com.dispatchai.delivery.infrastructure.client.fleet.dto;
import java.time.Instant;
import java.util.UUID;
public record ReservationResponse(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId,
    String status, Instant expiresAt, String releaseReason) {}
