package com.dispatchai.delivery.infrastructure.client.fleet.exception;

public class ReservationDeclinedException extends RuntimeException {
  public ReservationDeclinedException(String message) {
    super(message);
  }
}
