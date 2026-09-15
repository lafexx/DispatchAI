package com.dispatchai.fleet.domain.model.exceptions;

public class VehicleNotFoundException extends RuntimeException {
  public VehicleNotFoundException(String message) {
    super(message);
  }
}
