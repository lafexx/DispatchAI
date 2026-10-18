package com.dispatchai.agent.application.model;

public record DispatchAdviceDraft(
    String recommendedDriverId,
    String recommendedDriverName,
    String reasoning,
    String recommendedVehicleId) {
}
