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

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
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
