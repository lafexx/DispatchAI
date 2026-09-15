package com.dispatchai.fleet.presentation.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.dispatchai.fleet.domain.model.VehicleType;

public record VehicleResponse(
    UUID id,
    String registration,
    VehicleType type,
    BigDecimal capacityKg,
    boolean available) {
}
