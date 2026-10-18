package com.dispatchai.fleet.domain.exception;

public class ReservationConflictException extends RuntimeException {
  public ReservationConflictException(String message) {
    super(message);
  }
}
