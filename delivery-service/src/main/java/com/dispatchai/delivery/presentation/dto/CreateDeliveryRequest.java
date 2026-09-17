package com.dispatchai.delivery.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDeliveryRequest(
    @NotNull UUID customerId,
    @NotBlank String pickupAddress,
    @NotBlank String destinationAddress) {
}
