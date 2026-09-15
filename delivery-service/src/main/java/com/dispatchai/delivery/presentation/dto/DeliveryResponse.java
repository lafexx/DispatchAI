package com.dispatchai.delivery.presentation.dto;

import java.util.UUID;

public record DeliveryResponse(UUID id, UUID customerId, String pickupAddress, String destinationAddress,
    String status) {
}
