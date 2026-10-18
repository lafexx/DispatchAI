package com.dispatchai.fleet.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.dispatchai.fleet.domain.exception.ReservationConflictException;

@Entity
@Table(name = "fleet_reservations")
public class FleetReservation {
  @Id
  private UUID assignmentId;
  private UUID deliveryId;
  private UUID driverId;
  private UUID vehicleId;
  @Column(precision = 10, scale = 2)
  private java.math.BigDecimal parcelWeightKg;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private FleetReservationStatus status = FleetReservationStatus.INITIALIZING;
  private Instant expiresAt;
  private String releaseReason;

  protected FleetReservation() {
  }

  public FleetReservation(UUID assignmentId) {
    this.assignmentId = Objects.requireNonNull(assignmentId);
  }

  public void bind(UUID deliveryId, UUID driverId, UUID vehicleId) {
    Objects.requireNonNull(deliveryId, "Delivery ID is required");
    Objects.requireNonNull(driverId, "Driver ID is required");
    if (this.deliveryId != null && (!this.deliveryId.equals(deliveryId)
        || !this.driverId.equals(driverId) || !Objects.equals(this.vehicleId, vehicleId))) {
      throw new ReservationConflictException("Reservation ID belongs to different resources or delivery");
    }
    this.deliveryId = deliveryId;
    this.driverId = driverId;
    this.vehicleId = vehicleId;
  }

  public void bindWeight(java.math.BigDecimal weight) {
    if (weight == null || weight.signum() <= 0 || weight.scale() > 2 || weight.compareTo(new java.math.BigDecimal("99999999.99")) > 0) throw new ReservationConflictException("Parcel weight is required");
    if (parcelWeightKg != null && parcelWeightKg.compareTo(weight) != 0)
      throw new ReservationConflictException("Reservation token belongs to a different parcel weight");
    parcelWeightKg = weight;
  }
  public void verifyWeight(java.math.BigDecimal weight) {
    // Old active holds can finish during rollout; new weighted holds must match Delivery's authoritative value.
    if (parcelWeightKg != null && (weight == null || parcelWeightKg.compareTo(weight) != 0))
      throw new ReservationConflictException("Reservation weight does not match delivery");
  }
  public java.math.BigDecimal getParcelWeightKg() { return parcelWeightKg; }

  public void reserve(Instant deadline) {
    if (status != FleetReservationStatus.INITIALIZING) {
      throw new ReservationConflictException("Reservation cannot be acquired in state " + status);
    }
    expiresAt = Objects.requireNonNull(deadline);
    status = FleetReservationStatus.RESERVED;
  }

  public boolean expired(Instant now) {
    return status == FleetReservationStatus.RESERVED && !expiresAt.isAfter(now);
  }

  public void confirm(Instant now) {
    if (status == FleetReservationStatus.CONFIRMED) {
      return;
    }
    if (status != FleetReservationStatus.RESERVED || expired(now)) {
      throw new ReservationConflictException("Reservation is missing, released, or expired");
    }
    status = FleetReservationStatus.CONFIRMED;
  }

  /**
   * Compensation may release only unconfirmed work. Terminal events may release
   * both.
   */
  public boolean release(boolean terminalDelivery) {
    return release(terminalDelivery, terminalDelivery ? "TERMINAL_DELIVERY" : "COMPENSATED");
  }

  public boolean release(boolean terminalDelivery, String reason) {
    if (status == FleetReservationStatus.RELEASED) {
      return false;
    }
    if (status == FleetReservationStatus.CONFIRMED && !terminalDelivery) {
      throw new ReservationConflictException("Confirmed assignment requires a terminal delivery event");
    }
    boolean occupied = status.occupiesResources();
    status = FleetReservationStatus.RELEASED;
    releaseReason = Objects.requireNonNull(reason);
    return occupied;
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

  public FleetReservationStatus getStatus() {
    return status;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getReleaseReason() {
    return releaseReason;
  }
}
