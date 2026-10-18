package com.dispatchai.dispatch.infrastructure.messaging.consumer;

import com.dispatchai.dispatch.infrastructure.messaging.event.AssignmentRejectedEvent;
import com.dispatchai.dispatch.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.dispatch.infrastructure.messaging.event.DeliveryCompletedEvent;

import java.util.function.Consumer;
import org.springframework.context.annotation.*;
import org.springframework.messaging.Message;
import com.dispatchai.dispatch.application.DispatchApplicationService;
import com.dispatchai.dispatch.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;

@Configuration
public class AssignmentLifecycleEventConsumer {
  @Bean
  public Consumer<Message<AssignmentRejectedEvent>> assignmentRejected(DispatchApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("dispatch.assignmentRejected", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.rejectAssignment(event.assignmentId(), event.deliveryId(), event.reason(), event.deliveryStatus(), event.occurredAt()));
    };
  }
  @Bean
  public Consumer<Message<DeliveryCompletedEvent>> deliveryCompleted(DispatchApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("dispatch.deliveryCompleted", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.closeAssignment(event.deliveryId(), AssignmentStatus.COMPLETED, event.occurredAt()));
    };
  }
  @Bean
  public Consumer<Message<DeliveryCancelledEvent>> deliveryCancelled(DispatchApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("dispatch.deliveryCancelled", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.closeAssignment(event.deliveryId(), AssignmentStatus.CANCELLED, event.occurredAt()));
    };
  }
}
