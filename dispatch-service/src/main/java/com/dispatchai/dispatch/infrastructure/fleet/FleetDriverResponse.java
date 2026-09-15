package com.dispatchai.dispatch.infrastructure.fleet;

import java.util.UUID;

public record FleetDriverResponse(
    UUID id,
    String name,
    String availability) {
}
