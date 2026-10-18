package com.dispatchai.agent.presentation.dto;

import java.util.UUID;

public record DispatchAdviceResponse(
    UUID recommendedDriverId,
    String recommendedDriverName,
    String reasoning,
    UUID recommendedVehicleId,
    String recommendedVehicleRegistration,
    java.math.BigDecimal parcelWeightKg,
    java.math.BigDecimal vehicleCapacityKg,
    java.util.List<com.dispatchai.agent.application.model.DispatchCandidate> candidates,
    java.util.List<com.dispatchai.agent.infrastructure.client.fleet.dto.VehicleResponse> eligibleVehicles) {
}
