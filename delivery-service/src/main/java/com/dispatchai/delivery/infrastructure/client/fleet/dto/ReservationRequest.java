package com.dispatchai.delivery.infrastructure.client.fleet.dto;
import java.math.BigDecimal;
import java.util.UUID;
public record ReservationRequest(UUID deliveryId, UUID driverId, UUID vehicleId, BigDecimal parcelWeightKg) {}
