package com.dispatchai.dispatch.infrastructure.messaging.publisher;

import com.dispatchai.dispatch.infrastructure.messaging.event.DriverAssignedEvent;

import org.springframework.stereotype.Component;
import com.dispatchai.dispatch.infrastructure.messaging.outbox.OutboxWriter;

@Component
public class AssignmentEventPublisher {
  private final OutboxWriter outbox;
  public AssignmentEventPublisher(OutboxWriter outbox) { this.outbox = outbox; }
  public void publish(DriverAssignedEvent event) {
    outbox.enqueue("driverAssigned-out-0", event.eventId(), event.deliveryId(), event);
  }
}
