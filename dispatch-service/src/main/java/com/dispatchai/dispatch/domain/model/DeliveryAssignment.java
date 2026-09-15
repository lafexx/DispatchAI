package com.dispatchai.dispatch.domain.model;

import java.util.UUID;

import com.dispatchai.dispatch.domain.model.exceptions.InvalidAssignmentStateException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_assignments")
public class DeliveryAssignment {
  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private UUID deliveryId;

  @Column(nullable = false)
  private UUID driverId;

  private UUID vehicleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AssignmentStatus status;

  protected DeliveryAssignment() {
  }

  public DeliveryAssignment(UUID id, UUID deliveryId, UUID driverId, UUID vehicleId) {
    if (id == null) {
      throw new IllegalArgumentException("Assignment ID cannot be null");
    }

    if (deliveryId == null) {
      throw new IllegalArgumentException("Delivery ID cannot be null");
    }

    if (driverId == null) {
      throw new IllegalArgumentException("Driver ID cannot be null");
    }

    this.id = id;
    this.deliveryId = deliveryId;
    this.driverId = driverId;
    this.vehicleId = vehicleId;
    this.status = AssignmentStatus.ACTIVE;
  }

  public void complete() {
    if (status != AssignmentStatus.ACTIVE) {
      throw new InvalidAssignmentStateException(
          "Only active assignments can be completed");
    }

    status = AssignmentStatus.COMPLETED;
  }

  public void cancel() {
    if (status != AssignmentStatus.ACTIVE) {
      throw new InvalidAssignmentStateException(
          "Only active assignments can be cancelled");
    }

    status = AssignmentStatus.CANCELLED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getDeliveryId() {
    return deliveryId;
  }

  public UUID getDriverId() {
    return driverId;
  }

  public UUID getVehicleId() {
    return vehicleId;
  }

  public AssignmentStatus getStatus() {
    return status;
  }
}
