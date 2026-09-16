package com.dispatchai.customer.infrastructure.messaging;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dispatchai.customer.application.NotificationApplicationService;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.customer.infrastructure.messaging.event.DriverAssignedEvent;

@Configuration
public class CustomerEventConsumer {

  @Bean
  public Consumer<DriverAssignedEvent> driverAssigned(
      NotificationApplicationService notificationService) {

    return notificationService::notifyDriverAssigned;
  }

  @Bean
  public Consumer<DeliveryStartedEvent> deliveryStarted(
      NotificationApplicationService notificationService) {

    return notificationService::notifyDeliveryStarted;
  }

  @Bean
  public Consumer<DeliveryCompletedEvent> deliveryCompleted(
      NotificationApplicationService notificationService) {

    return notificationService::notifyDeliveryCompleted;
  }

  @Bean
  public Consumer<DeliveryCancelledEvent> deliveryCancelled(
      NotificationApplicationService notificationService) {

    return notificationService::notifyDeliveryCancelled;
  }
}
