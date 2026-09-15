package com.dispatchai.fleet.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehicles")
public class Vehicle {

  @Id
  private UUID id;

  @Column(nullable = false, unique = true)
  private String registration;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private VehicleType type;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal capacityKg;

  @Column(nullable = false)
  private boolean available;

  protected Vehicle() {
  }

  public Vehicle(
      UUID id,
      String registration,
      VehicleType type,
      BigDecimal capacityKg) {

    if (id == null) {
      throw new IllegalArgumentException("Vehicle ID cannot be null");
    }

    if (registration == null || registration.isBlank()) {
      throw new IllegalArgumentException("Registration cannot be blank");
    }

    if (type == null) {
      throw new IllegalArgumentException("Vehicle type cannot be null");
    }

    if (capacityKg == null || capacityKg.signum() <= 0) {
      throw new IllegalArgumentException(
          "Vehicle capacity must be greater than zero");
    }

    this.id = id;
    this.registration = registration;
    this.type = type;
    this.capacityKg = capacityKg;
    this.available = false;
  }

  public void markAvailable() {
    available = true;
  }

  public void markUnavailable() {
    available = false;
  }

  public UUID getId() {
    return id;
  }

  public String getRegistration() {
    return registration;
  }

  public VehicleType getType() {
    return type;
  }

  public BigDecimal getCapacityKg() {
    return capacityKg;
  }

  public boolean isAvailable() {
    return available;
  }
}
