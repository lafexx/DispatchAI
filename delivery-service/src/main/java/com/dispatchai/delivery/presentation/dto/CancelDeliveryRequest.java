package com.dispatchai.delivery.presentation.dto;

import com.dispatchai.delivery.domain.model.CancellationReason;

public record CancelDeliveryRequest(CancellationReason reason, String notes) {
}
