package com.dispatchai.fleet.domain.model;

public enum FleetReservationStatus {
  INITIALIZING, RESERVED, CONFIRMED, RELEASED;

  public boolean occupiesResources() {
    return this == RESERVED || this == CONFIRMED;
  }
}
