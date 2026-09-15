package com.dispatchai.dispatch.presentation.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AutoAssignRequest(
    @NotNull UUID deliveryId,
    UUID vehicleId) {
}
