package com.dispatchai.fleet.domain.model;

public enum FleetAssignmentStatus {
  AWAITING_ASSIGNMENT, ACTIVE, COMPLETED, CANCELLED;

  public boolean isTerminal() {
    return this == COMPLETED || this == CANCELLED;
  }
}
