package com.dispatchai.delivery.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {
  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID customerId;

  @Column(nullable = false)
  private String pickupAddress;

  @Column(nullable = false)
  private String destinationAddress;

  private UUID assignedDriverId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  protected Delivery() {
  }

  public Delivery(UUID id, UUID customerId, String pickupAddress, String destinationAddress) {
    this.id = id;
    this.customerId = customerId;
    this.pickupAddress = pickupAddress;
    this.destinationAddress = destinationAddress;
    this.status = DeliveryStatus.PENDING;
  }

  public void assign(UUID driverId) {
    if (status != DeliveryStatus.PENDING) {
      throw new InvalidDeliveryStateException("Only pending deliveries can be assigned");
    }

    if (driverId == null) {
      throw new IllegalArgumentException("Driver ID cannot be null");
    }

    assignedDriverId = driverId;
    status = DeliveryStatus.ASSIGNED;
  }

  public void start() {
    if (status != DeliveryStatus.ASSIGNED) {
      throw new InvalidDeliveryStateException("Only assigned deliveries can be started");
    }

    status = DeliveryStatus.IN_TRANSIT;
  }

  public void complete() {
    if (status != DeliveryStatus.IN_TRANSIT) {
      throw new InvalidDeliveryStateException("Only in-transit deliveries can be completed");
    }

    status = DeliveryStatus.COMPLETED;
  }

  public void cancel() {
    if (status == DeliveryStatus.COMPLETED) {
      throw new InvalidDeliveryStateException("Completed deliveries cannot be cancelled");
    }

    if (status == DeliveryStatus.CANCELLED) {
      throw new InvalidDeliveryStateException("Delivery is already cancelled");
    }

    status = DeliveryStatus.CANCELLED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getAssignedDriverId() {
    return assignedDriverId;
  }

  public String getPickupAddress() {
    return pickupAddress;
  }

  public String getDestinationAddress() {
    return destinationAddress;
  }

  public DeliveryStatus getStatus() {
    return status;
  }
}
