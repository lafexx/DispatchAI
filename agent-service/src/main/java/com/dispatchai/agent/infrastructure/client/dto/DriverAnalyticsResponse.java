package com.dispatchai.agent.infrastructure.client.dto;

import java.util.UUID;

public record DriverAnalyticsResponse(
    UUID driverId,
    long totalAssigned,
    long completed,
    long cancelled,
    long delayed,
    double averageDeliveryTimeSeconds,
    double averageTransitTimeSeconds) {
}
