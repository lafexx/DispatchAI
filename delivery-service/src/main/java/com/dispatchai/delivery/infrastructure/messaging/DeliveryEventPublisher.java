package com.dispatchai.delivery.infrastructure.messaging;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryCancelledEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryCompletedEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryCreatedEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryDelayedEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryStartedEvent;

@Component
public class DeliveryEventPublisher {

  private final StreamBridge streamBridge;

  public DeliveryEventPublisher(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  public void publish(DeliveryCreatedEvent event) {
    streamBridge.send("deliveryCreated-out-0", event);
  }

  public void publish(DeliveryStartedEvent event) {
    streamBridge.send("deliveryStarted-out-0", event);
  }

  public void publish(DeliveryCompletedEvent event) {
    streamBridge.send("deliveryCompleted-out-0", event);
  }

  public void publish(DeliveryCancelledEvent event) {
    streamBridge.send("deliveryCancelled-out-0", event);
  }

  public void publish(DeliveryDelayedEvent event) {
    streamBridge.send("deliveryDelayed-out-0", event);
  }
}
