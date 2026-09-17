package com.dispatchai.analytics.infrastructure.messaging;

import java.util.function.Function;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.dispatchai.analytics.domain.model.AnalyticsDeliveryStatus;
import com.dispatchai.analytics.domain.model.DeliveryAnalyticsState;
import com.dispatchai.analytics.domain.model.DeliveryStateUpdate;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCreatedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DriverAssignedEvent;

@Configuration
public class DeliveryAnalyticsStreams {

  @Bean
  public Function<KStream<String, DeliveryCreatedEvent>, KStream<String, DeliveryStateUpdate>> deliveryCreatedAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryStateUpdate(
            event.deliveryId(),
            event.customerId(),
            null,
            AnalyticsDeliveryStatus.PENDING,
            false,
            null,
            null,
            event.occurredAt()));
  }

  @Bean
  public Function<KStream<String, DriverAssignedEvent>, KStream<String, DeliveryStateUpdate>> driverAssignedAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryStateUpdate(
            event.deliveryId(),
            event.customerId(),
            event.driverId(),
            AnalyticsDeliveryStatus.ASSIGNED,
            null,
            null,
            null,
            event.occurredAt()));
  }

  @Bean
  public Function<KStream<String, DeliveryStartedEvent>, KStream<String, DeliveryStateUpdate>> deliveryStartedAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryStateUpdate(
            event.deliveryId(),
            event.customerId(),
            event.driverId(),
            AnalyticsDeliveryStatus.IN_TRANSIT,
            null,
            null,
            null,
            event.occurredAt()));
  }

  @Bean
  public Function<KStream<String, DeliveryDelayedEvent>, KStream<String, DeliveryStateUpdate>> deliveryDelayedAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryStateUpdate(
            event.deliveryId(),
            event.customerId(),
            event.driverId(),
            null,
            true,
            event.delayMinutes(),
            event.reason(),
            event.occurredAt()));
  }

  @Bean
  public Function<KStream<String, DeliveryCompletedEvent>, KStream<String, DeliveryStateUpdate>> deliveryCompletedAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryStateUpdate(
            event.deliveryId(),
            event.customerId(),
            event.driverId(),
            AnalyticsDeliveryStatus.COMPLETED,
            null,
            null,
            null,
            event.occurredAt()));
  }

  @Bean
  public Function<KStream<String, DeliveryCancelledEvent>, KStream<String, DeliveryStateUpdate>> deliveryCancelledAnalytics() {

    return input -> input
        .selectKey((key, event) -> event.deliveryId().toString())
        .mapValues(event -> new DeliveryStateUpdate(
            event.deliveryId(),
            event.customerId(),
            event.driverId(),
            AnalyticsDeliveryStatus.CANCELLED,
            null,
            null,
            null,
            event.occurredAt()));
  }

  @Bean
  public Function<KStream<String, DeliveryStateUpdate>, KTable<String, DeliveryAnalyticsState>> deliveryStateAggregator() {

    return input -> input
        .groupByKey()
        .aggregate(
            DeliveryAnalyticsState::empty,
            (deliveryId, update, state) -> state.apply(update),
            Materialized.as("delivery-state-store"));
  }
}
