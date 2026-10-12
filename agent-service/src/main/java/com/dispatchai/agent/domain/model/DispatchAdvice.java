package com.dispatchai.agent.domain.model;

import java.util.UUID;

public record DispatchAdvice(
    UUID recommendedDriverId,
    String recommendedDriverName,
    String reasoning) {
}
