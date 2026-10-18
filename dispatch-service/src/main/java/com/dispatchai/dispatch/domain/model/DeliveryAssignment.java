package com.dispatchai.dispatch.domain.model;

import java.time.Instant;
import java.util.UUID;

import com.dispatchai.dispatch.domain.exception.InvalidAssignmentStateException;

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

  @Column(nullable = false)
  private UUID deliveryId;

  @Column(nullable = false)
  private UUID driverId;

  private UUID vehicleId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AssignmentStatus status;

  private Instant assignedAt;
  private Instant completedAt;
  private Instant cancelledAt;
  private Instant rejectedAt;
  private String rejectionReason;
  @Column(nullable = false)
  private UUID assignmentEventId = UUID.randomUUID();

  protected DeliveryAssignment() {
  }

  public DeliveryAssignment(
      UUID id,
      UUID deliveryId,
      UUID driverId,
      UUID vehicleId) {

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
    this.assignedAt = Instant.now();
  }

  public void complete() {
    if (status != AssignmentStatus.ACTIVE) {
      throw new InvalidAssignmentStateException(
          "Only active assignments can be completed");
    }

    status = AssignmentStatus.COMPLETED;
    completedAt = Instant.now();
  }

  public void cancel() {
    if (status != AssignmentStatus.ACTIVE) {
      throw new InvalidAssignmentStateException(
          "Only active assignments can be cancelled");
    }

    status = AssignmentStatus.CANCELLED;
    cancelledAt = Instant.now();
  }

  /** Consumer transition: idempotent, with the producer's event timestamp. */
  public boolean applyTerminalState(AssignmentStatus terminal, Instant occurredAt) {
    if (terminal != AssignmentStatus.COMPLETED && terminal != AssignmentStatus.CANCELLED) {
      throw new IllegalArgumentException("A terminal assignment status is required");
    }
    java.util.Objects.requireNonNull(occurredAt, "Event time is required");
    if (status != AssignmentStatus.ACTIVE) {
      return false;
    }
    status = terminal;
    if (terminal == AssignmentStatus.COMPLETED) {
      completedAt = occurredAt;
    } else {
      cancelledAt = occurredAt;
    }
    return true;
  }

  public boolean reject(String reason, Instant occurredAt) {
    java.util.Objects.requireNonNull(reason);
    java.util.Objects.requireNonNull(occurredAt);
    if (status != AssignmentStatus.ACTIVE) {
      return false;
    }
    status = AssignmentStatus.REJECTED;
    rejectionReason = reason;
    rejectedAt = occurredAt;
    return true;
  }

  public UUID getAssignmentEventId() {
    return assignmentEventId;
  }

  public Instant getRejectedAt() {
    return rejectedAt;
  }

  public String getRejectionReason() {
    return rejectionReason;
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

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }
}
