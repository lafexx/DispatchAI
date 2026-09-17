package com.dispatchai.analytics.presentation.dto;

public record DeliverySummaryResponse(
    long total,
    long pending,
    long assigned,
    long inTransit,
    long completed,
    long cancelled,
    long delayed,
    double averageDelayMinutes,
    double averageAssignmentTimeSeconds,
    double averageDeliveryTimeSeconds,
    double averageTransitTimeSeconds,
    double completionRate,
    double cancellationRate,
    double delayRate) {
}
