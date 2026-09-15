package com.dispatchai.fleet.domain.model.exceptions;

public class DriverNotFoundException extends RuntimeException {
  public DriverNotFoundException(String message) {
    super(message);
  }
}
