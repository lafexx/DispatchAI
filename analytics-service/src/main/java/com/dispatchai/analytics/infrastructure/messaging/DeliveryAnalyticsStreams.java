package com.dispatchai.analytics.infrastructure.messaging;

import java.util.function.Function;

import org.apache.kafka.streams.kstream.KStream;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dispatchai.analytics.domain.model.AnalyticsDeliveryStatus;
import com.dispatchai.analytics.domain.model.DeliveryAnalyticsState;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCreatedEvent;

@Configuration
public class DeliveryAnalyticsStreams {

  @Bean
  public Function<KStream<String, DeliveryCreatedEvent>, KStream<String, DeliveryAnalyticsState>> deliveryCreatedAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryAnalyticsState(
            event.deliveryId(),
            event.customerId(),
            null,
            AnalyticsDeliveryStatus.PENDING,
            false,
            null,
            null,
            event.occurredAt(),
            null,
            null));
  }
}
