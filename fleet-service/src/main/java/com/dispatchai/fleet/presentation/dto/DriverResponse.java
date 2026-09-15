package com.dispatchai.fleet.presentation.dto;

import java.util.UUID;

import com.dispatchai.fleet.domain.model.DriverAvailability;

public record DriverResponse(UUID id, String name, DriverAvailability availability) {
}
