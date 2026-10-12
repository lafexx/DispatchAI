// package com.dispatchai.dispatch.infrastructure.messaging;
//
// import java.util.function.Consumer;
//
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
//
// import com.dispatchai.dispatch.application.DispatchApplicationService;
// import com.dispatchai.dispatch.infrastructure.delivery.DeliveryCreatedEvent;
//
// @Configuration
// public class DeliveryEventConsumer {
//
// @Bean
// public Consumer<DeliveryCreatedEvent> deliveryCreated(
// DispatchApplicationService dispatchService) {
//
// return event -> dispatchService.autoAssign(
// event.deliveryId(),
// null);
// }
// }
