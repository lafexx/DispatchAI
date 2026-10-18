package com.dispatchai.customer.infrastructure.messaging.consumer;

import java.util.function.Consumer;
import org.springframework.context.annotation.*;
import org.springframework.messaging.Message;
import com.dispatchai.customer.application.NotificationApplicationService;
import com.dispatchai.customer.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryAssignedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCompletedEvent;

@Configuration
public class CustomerEventConsumer {
  @Bean
  public Consumer<Message<DeliveryAssignedEvent>> driverAssigned(NotificationApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("customer.driverAssigned", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.notifyDriverAssigned(event));
    };
  }
  @Bean
  public Consumer<Message<DeliveryStartedEvent>> deliveryStarted(NotificationApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("customer.deliveryStarted", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.notifyDeliveryStarted(event));
    };
  }
  @Bean
  public Consumer<Message<DeliveryCompletedEvent>> deliveryCompleted(NotificationApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("customer.deliveryCompleted", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.notifyDeliveryCompleted(event));
    };
  }
  @Bean
  public Consumer<Message<DeliveryCancelledEvent>> deliveryCancelled(NotificationApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("customer.deliveryCancelled", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.notifyDeliveryCancelled(event));
    };
  }
  @Bean
  public Consumer<Message<DeliveryDelayedEvent>> deliveryDelayed(NotificationApplicationService service, InboxProcessor inbox) {
    return message -> {
      var event = message.getPayload();
      inbox.process("customer.deliveryDelayed", event.eventId(), event.deliveryId(), message.getHeaders(),
          () -> service.notifyDeliveryDelayed(event));
    };
  }
}
