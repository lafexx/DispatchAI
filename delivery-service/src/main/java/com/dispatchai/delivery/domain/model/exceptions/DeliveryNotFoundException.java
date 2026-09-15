package com.dispatchai.delivery.domain.model.exceptions;

public class DeliveryNotFoundException extends RuntimeException {
  public DeliveryNotFoundException(String message) {
    super(message);
  }
}
