package com.dispatchai.dispatch.infrastructure.client.fleet.dto;

import java.util.UUID;

public record DriverResponse(
    UUID id,
    String name,
    String availability) {
}
