package com.dispatchai.agent.presentation.dto;

import java.util.UUID;

public record DispatchAdviceResponse(
    UUID recommendedDriverId,
    String recommendedDriverName,
    String reasoning) {
}
