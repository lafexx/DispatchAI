
package com.dispatchai.dispatch.infrastructure.client.fleet.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId,
    String status, Instant expiresAt, String releaseReason) {
  public ReservationResponse(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId,
      String status, Instant expiresAt) {
    this(assignmentId, deliveryId, driverId, vehicleId, status, expiresAt, null);
  }
}
