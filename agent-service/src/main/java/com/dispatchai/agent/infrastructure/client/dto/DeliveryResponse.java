package com.dispatchai.agent.infrastructure.client.dto;

import java.util.UUID;

public record DeliveryResponse(
    UUID id,
    UUID customerId,
    UUID assignedDriverId,
    String pickupAddress,
    String destinationAddress,
    String status,
    boolean delayed,
    Integer delayMinutes,
    String delayReason) {
}
