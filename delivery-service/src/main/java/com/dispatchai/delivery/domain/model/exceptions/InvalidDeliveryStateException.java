package com.dispatchai.delivery.domain.model.exceptions;

public class InvalidDeliveryStateException extends RuntimeException {
  public InvalidDeliveryStateException(String message) {
    super(message);
  }
}
