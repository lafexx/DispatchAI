package com.dispatchai.dispatch.infrastructure.messaging;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class AssignmentEventPublisher {

  private final StreamBridge streamBridge;

  public AssignmentEventPublisher(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  public void publish(DriverAssignedEvent event) {
    streamBridge.send("driverAssigned-out-0", event);
  }
}
