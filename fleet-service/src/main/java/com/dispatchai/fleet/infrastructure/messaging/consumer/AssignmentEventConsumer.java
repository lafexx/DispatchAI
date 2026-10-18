package com.dispatchai.fleet.infrastructure.messaging.consumer;

import com.dispatchai.fleet.infrastructure.messaging.event.DeliveryAssignedEvent;
import com.dispatchai.fleet.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.fleet.infrastructure.messaging.event.DeliveryCompletedEvent;

import java.util.function.Consumer;
import org.springframework.context.annotation.*;
import org.springframework.messaging.Message;
import com.dispatchai.fleet.application.FleetAssignmentApplicationService;
import com.dispatchai.fleet.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.fleet.domain.model.FleetAssignmentStatus;

@Configuration
public class AssignmentEventConsumer {
  @Bean
  public Consumer<Message<DeliveryAssignedEvent>> driverAssigned(FleetAssignmentApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("fleet.driverAssigned", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.assigned(event.deliveryId(), event.assignmentId(), event.driverId(), event.vehicleId(), event.occurredAt()));
    };
  }
  @Bean
  public Consumer<Message<DeliveryCompletedEvent>> deliveryCompleted(FleetAssignmentApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("fleet.deliveryCompleted", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.closed(event.deliveryId(), FleetAssignmentStatus.COMPLETED, event.occurredAt()));
    };
  }
  @Bean
  public Consumer<Message<DeliveryCancelledEvent>> deliveryCancelled(FleetAssignmentApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("fleet.deliveryCancelled", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.closed(event.deliveryId(), FleetAssignmentStatus.CANCELLED, event.occurredAt()));
    };
  }
}
