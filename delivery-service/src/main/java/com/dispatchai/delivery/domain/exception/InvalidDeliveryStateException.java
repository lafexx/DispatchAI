package com.dispatchai.delivery.domain.exception;

public class InvalidDeliveryStateException extends RuntimeException {
  public InvalidDeliveryStateException(String message) {
    super(message);
  }
}
