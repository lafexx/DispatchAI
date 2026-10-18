package com.dispatchai.delivery.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DelayDeliveryRequest(
    @Positive int delayMinutes,
    @NotBlank String reason) {
}
