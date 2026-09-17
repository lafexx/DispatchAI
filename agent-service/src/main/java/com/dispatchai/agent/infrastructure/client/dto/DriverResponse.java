package com.dispatchai.agent.infrastructure.client.dto;

import java.util.UUID;

public record DriverResponse(
    UUID id,
    String name,
    String availability) {
}
