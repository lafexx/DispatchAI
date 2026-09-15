package com.dispatchai.delivery.infrastructure.messaging;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventPublisher {

  private final StreamBridge streamBridge;

  public DeliveryEventPublisher(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  public void publish(DeliveryCreatedEvent event) {
    streamBridge.send("deliveryCreated-out-0", event);
  }
}
