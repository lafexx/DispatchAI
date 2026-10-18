package com.dispatchai.agent.presentation.dto;

import java.util.UUID;

public record DispatchAdviceRequest(
    @jakarta.validation.constraints.NotNull UUID deliveryId) {
}
