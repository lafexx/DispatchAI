package com.dispatchai.fleet.presentation.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record ReservationRequest(@NotNull UUID deliveryId, @NotNull UUID driverId, UUID vehicleId,
    @jakarta.validation.constraints.DecimalMin("0.01")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2) java.math.BigDecimal parcelWeightKg) {
  public ReservationRequest(UUID deliveryId, UUID driverId, UUID vehicleId) { this(deliveryId, driverId, vehicleId, java.math.BigDecimal.ONE); }
}
