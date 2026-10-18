package com.dispatchai.dispatch.infrastructure.client.delivery.dto;

import java.util.UUID;


public record DeliveryResponse(
    UUID id,
    UUID customerId,
    UUID assignedDriverId,
    String pickupAddress,
    String destinationAddress,
    RemoteDeliveryStatus status, java.math.BigDecimal parcelWeightKg,
    boolean delayed, Integer delayMinutes, String delayReason) {
  public DeliveryResponse(UUID id, UUID customerId, UUID assignedDriverId, String pickupAddress,
      String destinationAddress, RemoteDeliveryStatus status, java.math.BigDecimal parcelWeightKg) {
    this(id, customerId, assignedDriverId, pickupAddress, destinationAddress, status, parcelWeightKg, false, null, null);
  }
  public DeliveryResponse(UUID id, UUID customerId, UUID assignedDriverId, String pickupAddress,
      String destinationAddress, RemoteDeliveryStatus status) {
    this(id, customerId, assignedDriverId, pickupAddress, destinationAddress, status, java.math.BigDecimal.ONE);
  }
}
