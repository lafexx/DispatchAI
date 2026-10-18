package com.dispatchai.dispatch.presentation.dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.DriverResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
public record DispatchEligibilityResponse(UUID deliveryId, BigDecimal parcelWeightKg,
    List<DriverResponse> availableDrivers, List<VehicleResponse> eligibleVehicles) {}
