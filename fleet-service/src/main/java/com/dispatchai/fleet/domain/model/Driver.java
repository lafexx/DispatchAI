package com.dispatchai.fleet.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drivers")
public class Driver {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private DriverAvailability availability;

  @Column(nullable = false)
  private int activeAssignmentCount;

  public void assignmentStarted() { activeAssignmentCount++; }

  public void assignmentFinished() {
    if (activeAssignmentCount <= 0) {
      throw new IllegalStateException("Assignment occupancy is already zero");
    }
    activeAssignmentCount--;
  }

  public int getActiveAssignmentCount() { return activeAssignmentCount; }

  protected Driver() {
  }

  public Driver(UUID id, String name) {
    if (id == null) {
      throw new IllegalArgumentException("Driver ID cannot be null");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Driver name cannot be blank");
    }

    this.id = id;
    this.name = name;
    this.availability = DriverAvailability.UNAVAILABLE;
  }

  public void markAvailable() {
    availability = DriverAvailability.AVAILABLE;
  }

  public void markUnavailable() {
    availability = DriverAvailability.UNAVAILABLE;
  }

  public boolean isAvailable() {
    return getAvailability() == DriverAvailability.AVAILABLE;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public DriverAvailability getAvailability() {
    return activeAssignmentCount > 0 ? DriverAvailability.UNAVAILABLE : availability;
  }
}
