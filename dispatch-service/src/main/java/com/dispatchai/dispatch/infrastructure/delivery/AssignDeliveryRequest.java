package com.dispatchai.dispatch.infrastructure.delivery;

import java.util.UUID;

public record AssignDeliveryRequest(
    UUID driverId) {
}
