package com.dispatchai.agent.domain.model;

import java.util.List;

import com.dispatchai.agent.infrastructure.client.dto.DeliveryResponse;

public record DispatchRecommendationContext(
    DeliveryResponse delivery,
    List<DispatchCandidate> availableDrivers) {
}
