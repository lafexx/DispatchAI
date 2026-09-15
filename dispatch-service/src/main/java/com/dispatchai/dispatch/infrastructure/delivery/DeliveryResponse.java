package com.dispatchai.dispatch.infrastructure.delivery;

import java.util.UUID;

import com.dispatchai.dispatch.domain.model.RemoteDeliveryStatus;

public record DeliveryResponse(
    UUID id,
    UUID customerId,
    UUID assignedDriverId,
    String pickupAddress,
    String destinationAddress,
    RemoteDeliveryStatus status) {
}
