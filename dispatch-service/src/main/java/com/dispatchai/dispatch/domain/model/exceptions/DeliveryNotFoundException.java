package com.dispatchai.dispatch.domain.model.exceptions;

import java.util.UUID;

public class DeliveryNotFoundException extends RuntimeException {

  public DeliveryNotFoundException(UUID deliveryId) {
    super("Delivery not found: " + deliveryId);
  }
}
