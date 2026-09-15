package com.dispatchai.dispatch.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateAssignmentRequest(
    @NotNull UUID deliveryId,
    @NotNull UUID driverId,
    UUID vehicleId) {
}
