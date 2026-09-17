package com.dispatchai.agent.infrastructure.client.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryAnalyticsResponse(
    UUID deliveryId,
    UUID customerId,
    UUID driverId,
    String status,
    boolean delayed,
    Integer delayMinutes,
    String delayReason,
    Instant createdAt,
    Instant assignedAt,
    Instant startedAt,
    Instant completedAt,
    Instant cancelledAt) {
}
