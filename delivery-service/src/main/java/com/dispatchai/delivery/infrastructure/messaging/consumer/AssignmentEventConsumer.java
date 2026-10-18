package com.dispatchai.delivery.infrastructure.messaging.consumer;

import java.util.function.Consumer;
import org.springframework.context.annotation.*;
import org.springframework.messaging.Message;
import com.dispatchai.delivery.application.DeliveryApplicationService;
import com.dispatchai.delivery.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.delivery.infrastructure.messaging.event.DriverAssignedEvent;

@Configuration
public class AssignmentEventConsumer {
  @Bean
  public Consumer<Message<DriverAssignedEvent>> driverAssigned(DeliveryApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("delivery.driverAssigned", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.acceptAssignment(event));
    };
  }
}
