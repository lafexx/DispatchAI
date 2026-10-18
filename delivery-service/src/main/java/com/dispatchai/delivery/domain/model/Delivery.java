package com.dispatchai.delivery.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

import com.dispatchai.delivery.domain.exception.InvalidDeliveryStateException;

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

  @Column(precision = 10, scale = 2)
  private java.math.BigDecimal parcelWeightKg;
  private UUID assignedAssignmentId;
  private UUID assignedVehicleId;
  private Instant assignmentAcceptedAt;

  @Column(nullable = true)
  private CancellationReason cancellationReason;

  @Column(nullable = true)
  private String cancellationNotes;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DeliveryStatus status;

  private boolean delayed;

  private Integer delayMinutes;

  private String delayReason;

  private Instant delayedAt;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Delivery() {
  }

  public Delivery(UUID id, UUID customerId, String pickupAddress, String destinationAddress) {
    this(id, customerId, pickupAddress, destinationAddress, java.math.BigDecimal.ONE);
  }

  public Delivery(UUID id, UUID customerId, String pickupAddress, String destinationAddress, java.math.BigDecimal weight) {
    if (weight == null || weight.signum() <= 0 || weight.scale() > 2 || weight.compareTo(new java.math.BigDecimal("99999999.99")) > 0)
      throw new IllegalArgumentException("Parcel weight must be positive, at most 99999999.99 kg, with at most two decimal places");
    this.parcelWeightKg = weight;
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

  /**
   * Event application is tolerant of duplicates and terminal-before-assignment.
   */
  public boolean applyAssignment(UUID driverId) {
    if (driverId == null) {
      throw new IllegalArgumentException("Driver ID cannot be null");
    }
    if (status == DeliveryStatus.COMPLETED || status == DeliveryStatus.CANCELLED) {
      return false;
    }
    if (status == DeliveryStatus.PENDING) {
      assign(driverId);
      return true;
    }
    if (driverId.equals(assignedDriverId)) {
      return false;
    }
    throw new InvalidDeliveryStateException("Delivery already belongs to a different driver");
  }

  public boolean hasAssignment(UUID assignmentId, UUID driverId, UUID vehicleId) {
    return assignmentId != null && assignmentId.equals(assignedAssignmentId)
        && java.util.Objects.equals(driverId, assignedDriverId)
        && java.util.Objects.equals(vehicleId, assignedVehicleId);
  }

  public void acceptReservedAssignment(UUID assignmentId, UUID driverId, UUID vehicleId, Instant at) {
    java.util.Objects.requireNonNull(assignmentId);
    java.util.Objects.requireNonNull(at);
    assign(driverId);
    assignedAssignmentId = assignmentId;
    assignedVehicleId = vehicleId;
    assignmentAcceptedAt = at;
  }

  public Instant getAssignmentAcceptedAt() {
    return assignmentAcceptedAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
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

  public void markDelayed(
      int delayMinutes,
      String reason) {

    if (status == DeliveryStatus.COMPLETED) {
      throw new InvalidDeliveryStateException(
          "Completed delivery cannot be delayed");
    }

    if (status == DeliveryStatus.CANCELLED) {
      throw new InvalidDeliveryStateException(
          "Cancelled delivery cannot be delayed");
    }

    if (delayMinutes <= 0) {
      throw new IllegalArgumentException(
          "Delay minutes must be greater than zero");
    }

    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException(
          "Delay reason cannot be blank");
    }

    this.delayed = true;
    this.delayMinutes = delayMinutes;
    this.delayReason = reason;
    this.delayedAt = Instant.now();
  }

  public void setParcelWeightKg(java.math.BigDecimal weight) {
    if (status != DeliveryStatus.PENDING) throw new InvalidDeliveryStateException("Only pending deliveries can change parcel weight");
    if (weight == null || weight.signum() <= 0 || weight.scale() > 2 || weight.compareTo(new java.math.BigDecimal("99999999.99")) > 0)
      throw new IllegalArgumentException("Invalid parcel weight");
    parcelWeightKg = weight;
    updatedAt = Instant.now();
  }

  public java.math.BigDecimal getParcelWeightKg() { return parcelWeightKg; }

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

  public boolean isDelayed() {
    return delayed;
  }

  public Integer getDelayMinutes() {
    return delayMinutes;
  }

  public String getDelayReason() {
    return delayReason;
  }

  public Instant getDelayedAt() {
    return delayedAt;
  }
}
