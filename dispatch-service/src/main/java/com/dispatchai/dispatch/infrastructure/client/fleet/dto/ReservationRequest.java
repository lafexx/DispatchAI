
package com.dispatchai.dispatch.infrastructure.client.fleet.dto;

import java.util.UUID;

public record ReservationRequest(UUID deliveryId, UUID driverId, UUID vehicleId, java.math.BigDecimal parcelWeightKg) {
  public ReservationRequest(UUID deliveryId, UUID driverId, UUID vehicleId) { this(deliveryId, driverId, vehicleId, java.math.BigDecimal.ONE); }
}
