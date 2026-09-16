package com.dispatchai.delivery.presentation.dto;

import java.util.UUID;

import com.dispatchai.delivery.domain.model.DeliveryStatus;

public record DeliveryResponse(
    UUID id,
    UUID customerId,
    String pickupAddress,
    String destinationAddress,
    DeliveryStatus status,
    UUID assignedDriverId,
    boolean delayed,
    Integer delayMinutes,
    String delayReason) {
}
