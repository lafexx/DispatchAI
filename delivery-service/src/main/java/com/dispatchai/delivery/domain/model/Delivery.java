package com.dispatchai.delivery.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
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

  @Column(nullable = true)
  private UUID assignedDriverId;

  @Column(nullable = true)
  private CancellationReason cancellationReason;

  @Column(nullable = true)
  private String cancellationNotes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Delivery() {
  }

  public Delivery(UUID id, UUID customerId, String pickupAddress, String destinationAddress) {
    this.id = id;
    this.customerId = customerId;
    this.pickupAddress = pickupAddress;
    this.destinationAddress = destinationAddress;
    this.status = DeliveryStatus.PENDING;

    var now = Instant.now();

    this.createdAt = now;
    this.updatedAt = now;
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
    updatedAt = Instant.now();
  }

  public void start() {
    if (status != DeliveryStatus.ASSIGNED) {
      throw new InvalidDeliveryStateException("Only assigned deliveries can be started");
    }

    status = DeliveryStatus.IN_TRANSIT;
    updatedAt = Instant.now();
  }

  public void complete() {
    if (status != DeliveryStatus.IN_TRANSIT) {
      throw new InvalidDeliveryStateException("Only in-transit deliveries can be completed");
    }

    status = DeliveryStatus.COMPLETED;
    updatedAt = Instant.now();
  }

  public void cancel(CancellationReason reason, String notes) {
    if (status == DeliveryStatus.COMPLETED) {
      throw new InvalidDeliveryStateException("Completed deliveries cannot be cancelled");
    }

    if (status == DeliveryStatus.CANCELLED) {
      throw new InvalidDeliveryStateException("Delivery is already cancelled");
    }

    this.cancellationReason = reason;
    this.cancellationNotes = notes;
    status = DeliveryStatus.CANCELLED;
    updatedAt = Instant.now();
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
