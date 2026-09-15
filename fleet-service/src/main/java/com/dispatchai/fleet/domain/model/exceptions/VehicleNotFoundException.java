package com.dispatchai.fleet.domain.model.exceptions;

import java.util.UUID;

public class VehicleNotFoundException extends RuntimeException {

  public VehicleNotFoundException(UUID vehicleId) {
    super("Vehicle not found: " + vehicleId);
  }
}
