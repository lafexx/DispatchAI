package com.dispatchai.delivery.domain.model;

public class InvalidDeliveryStateException extends RuntimeException {
  public InvalidDeliveryStateException(String message) {
    super(message);
  }
}
