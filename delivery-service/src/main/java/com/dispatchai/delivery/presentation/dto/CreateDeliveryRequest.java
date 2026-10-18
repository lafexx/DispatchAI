package com.dispatchai.delivery.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliveryRequest(
    @NotNull UUID customerId,
    @NotBlank String pickupAddress,
    @NotBlank String destinationAddress,
    @NotNull @jakarta.validation.constraints.DecimalMin("0.01")
    @jakarta.validation.constraints.Digits(integer=8, fraction=2) java.math.BigDecimal parcelWeightKg) {
  public CreateDeliveryRequest(UUID customerId, String pickupAddress, String destinationAddress) {
    this(customerId, pickupAddress, destinationAddress, java.math.BigDecimal.ONE);
  }
}
