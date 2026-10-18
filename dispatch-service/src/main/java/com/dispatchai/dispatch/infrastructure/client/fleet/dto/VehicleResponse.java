package com.dispatchai.dispatch.infrastructure.client.fleet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VehicleResponse(UUID id, String registration, String type, BigDecimal capacityKg, boolean available) {}
