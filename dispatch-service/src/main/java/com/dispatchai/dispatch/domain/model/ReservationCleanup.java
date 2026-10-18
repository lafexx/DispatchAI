package com.dispatchai.dispatch.domain.model;

import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation_cleanups")
public class ReservationCleanup {
  @Id
  private UUID assignmentId;
  private UUID deliveryId;
  private UUID driverId;
  private UUID vehicleId;

  protected ReservationCleanup() {
  }

  public ReservationCleanup(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId) {
    this.assignmentId = assignmentId;
    this.deliveryId = deliveryId;
    this.driverId = driverId;
    this.vehicleId = vehicleId;
  }

  public UUID getAssignmentId() {
    return assignmentId;
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
}
