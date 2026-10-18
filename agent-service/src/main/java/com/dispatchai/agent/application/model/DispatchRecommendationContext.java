package com.dispatchai.agent.application.model;

import java.util.List;

import com.dispatchai.agent.infrastructure.client.delivery.dto.DeliveryResponse;

public record DispatchRecommendationContext(
    DeliveryResponse delivery,
    List<DispatchCandidate> availableDrivers,
    List<com.dispatchai.agent.infrastructure.client.fleet.dto.VehicleResponse> eligibleVehicles) {
}
