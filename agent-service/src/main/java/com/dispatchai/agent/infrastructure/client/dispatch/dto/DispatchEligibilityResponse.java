package com.dispatchai.agent.infrastructure.client.dispatch.dto;

import com.dispatchai.agent.infrastructure.client.fleet.dto.DriverResponse;
import com.dispatchai.agent.infrastructure.client.fleet.dto.VehicleResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
public record DispatchEligibilityResponse(UUID deliveryId, BigDecimal parcelWeightKg,
    List<DriverResponse> availableDrivers, List<VehicleResponse> eligibleVehicles) {}
