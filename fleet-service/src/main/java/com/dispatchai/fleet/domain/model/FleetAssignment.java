package com.dispatchai.fleet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fleet's local projection, not ownership of Dispatch's assignment decision. */
@Entity
@Table(name = "fleet_assignments")
public class FleetAssignment {
  @Id
  private UUID deliveryId;
  private UUID assignmentId;
  private UUID driverId;
  private UUID vehicleId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FleetAssignmentStatus status = FleetAssignmentStatus.AWAITING_ASSIGNMENT;
  private Instant assignedAt;
  private Instant closedAt;

  protected FleetAssignment() {}

  public FleetAssignment(UUID deliveryId) {
    this.deliveryId = Objects.requireNonNull(deliveryId, "Delivery ID is required");
  }

  /** Returns true only when occupancy must be added, once. */
  public boolean recordAssignment(UUID assignmentId, UUID driverId, UUID vehicleId, Instant occurredAt) {
    Objects.requireNonNull(assignmentId, "Assignment ID is required");
    Objects.requireNonNull(driverId, "Driver ID is required");
    Objects.requireNonNull(occurredAt, "Event time is required");
    if (this.assignmentId != null) {
      if (!this.assignmentId.equals(assignmentId) || !this.driverId.equals(driverId)
          || !Objects.equals(this.vehicleId, vehicleId)) {
        throw new IllegalStateException("Conflicting assignment for delivery " + deliveryId);
      }
      return false;
    }
    this.assignmentId = assignmentId;
    this.driverId = driverId;
    this.vehicleId = vehicleId;
    this.assignedAt = occurredAt;
    if (status.isTerminal()) {
      return false; // Terminal event arrived first: retain metadata, never occupy resources.
    }
    status = FleetAssignmentStatus.ACTIVE;
    return true;
  }

  /** Returns true only when previously applied occupancy must be removed. */
  public boolean recordTerminal(FleetAssignmentStatus terminal, Instant occurredAt) {
    if (terminal == null || !terminal.isTerminal()) {
      throw new IllegalArgumentException("A terminal assignment status is required");
    }
    Objects.requireNonNull(occurredAt, "Event time is required");
    if (status.isTerminal()) { return false; }
    boolean wasActive = status == FleetAssignmentStatus.ACTIVE;
    status = terminal;
    closedAt = occurredAt;
    return wasActive;
  }

  public UUID getDriverId() { return driverId; }
  public UUID getVehicleId() { return vehicleId; }
  public FleetAssignmentStatus getStatus() { return status; }
  public Instant getClosedAt() { return closedAt; }
}
