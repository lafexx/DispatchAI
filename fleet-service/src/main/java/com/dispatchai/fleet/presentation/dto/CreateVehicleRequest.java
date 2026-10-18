package com.dispatchai.fleet.presentation.dto;

import java.math.BigDecimal;

import com.dispatchai.fleet.domain.model.VehicleType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateVehicleRequest(
    @NotBlank String registration,
    @NotNull VehicleType type,
    @NotNull @Positive BigDecimal capacityKg) {
}
