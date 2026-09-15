package com.dispatchai.delivery.infrastructure.messaging;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dispatchai.delivery.application.DeliveryApplicationService;

@Configuration
public class AssignmentEventConsumer {

  @Bean
  public Consumer<DriverAssignedEvent> driverAssigned(
      DeliveryApplicationService deliveryService) {

    return event -> deliveryService.assign(
        event.deliveryId(),
        event.driverId());
  }
}
