package com.dispatchai.delivery.infrastructure.messaging.publisher;

import org.springframework.stereotype.Component;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryAssignedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCreatedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.AssignmentRejectedEvent;
import com.dispatchai.delivery.infrastructure.messaging.outbox.OutboxWriter;

@Component
public class DeliveryEventPublisher {
  private final OutboxWriter outbox;
  public DeliveryEventPublisher(OutboxWriter outbox) { this.outbox = outbox; }
  public void publish(DeliveryCreatedEvent event) {
    outbox.enqueue("deliveryCreated-out-0", event.eventId(), event.deliveryId(), event);
  }
  public void publish(DeliveryStartedEvent event) {
    outbox.enqueue("deliveryStarted-out-0", event.eventId(), event.deliveryId(), event);
  }
  public void publish(DeliveryCompletedEvent event) {
    outbox.enqueue("deliveryCompleted-out-0", event.eventId(), event.deliveryId(), event);
  }
  public void publish(DeliveryCancelledEvent event) {
    outbox.enqueue("deliveryCancelled-out-0", event.eventId(), event.deliveryId(), event);
  }
  public void publish(DeliveryDelayedEvent event) {
    outbox.enqueue("deliveryDelayed-out-0", event.eventId(), event.deliveryId(), event);
  }
  public void publish(DeliveryAssignedEvent event) {
    outbox.enqueue("deliveryAssigned-out-0", event.eventId(), event.deliveryId(), event);
  }
  public void publish(AssignmentRejectedEvent event) {
    outbox.enqueue("assignmentRejected-out-0", event.eventId(), event.deliveryId(), event);
  }
}
