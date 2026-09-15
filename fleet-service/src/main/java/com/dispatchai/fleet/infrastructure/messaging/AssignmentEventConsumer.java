package com.dispatchai.fleet.infrastructure.messaging;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dispatchai.fleet.application.DriverApplicationService;

@Configuration
public class AssignmentEventConsumer {

  @Bean
  public Consumer<DriverAssignedEvent> driverAssigned(
      DriverApplicationService driverService) {

    return event -> driverService.markUnavailable(event.driverId());
  }
}
