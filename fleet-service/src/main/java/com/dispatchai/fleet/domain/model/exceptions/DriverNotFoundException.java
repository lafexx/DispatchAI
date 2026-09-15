package com.dispatchai.fleet.domain.model.exceptions;

import java.util.UUID;

public class DriverNotFoundException extends RuntimeException {

  public DriverNotFoundException(UUID driverId) {
    super("Driver not found: " + driverId);
  }
}
