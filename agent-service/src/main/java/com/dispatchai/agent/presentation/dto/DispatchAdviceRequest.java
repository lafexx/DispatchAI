package com.dispatchai.agent.presentation.dto;

import java.util.UUID;

public record DispatchAdviceRequest(
    UUID deliveryId) {
}
