package com.dispatchai.analytics.infrastructure.messaging.streams;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;

import com.dispatchai.analytics.domain.model.AnalyticsDeliveryStatus;
import com.dispatchai.analytics.domain.model.DeliveryAnalyticsState;
import com.dispatchai.analytics.domain.model.DeliveryStateUpdate;
import com.dispatchai.analytics.domain.model.DriverPerformanceState;
import com.dispatchai.analytics.domain.model.SummaryAggregate;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryCreatedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.analytics.infrastructure.messaging.event.DeliveryAssignedEvent;

@Configuration
public class DeliveryAnalyticsStreams {

  @Bean
  public org.apache.kafka.streams.processor.TimestampExtractor analyticsEventTimestampExtractor() {
    return new AnalyticsEventTimestampExtractor();
  }

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
            event.occurredAt(),
            event.eventId()));
  }

  @Bean
  public Function<KStream<String, DeliveryAssignedEvent>, KStream<String, DeliveryStateUpdate>> driverAssignedAnalytics() {

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
            event.occurredAt(),
            event.eventId()));
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
            event.occurredAt(),
            event.eventId()));
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
            event.occurredAt(),
            event.eventId()));
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
            event.occurredAt(),
            event.eventId()));
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
            event.occurredAt(),
            event.eventId()));
  }

  @Bean
  public org.apache.kafka.streams.state.StoreBuilder<KeyValueStore<String, Boolean>> processedDeliveryEvents() {
    return org.apache.kafka.streams.state.Stores.keyValueStoreBuilder(
        org.apache.kafka.streams.state.Stores.persistentKeyValueStore(DeliveryEventDeduplicator.STORE),
        Serdes.String(), Serdes.Boolean());
  }

  @Bean
  public Consumer<KStream<String, DeliveryStateUpdate>> deliveryStateAggregator() {

    var deliveryStateSerde = new JacksonJsonSerde<>(DeliveryAnalyticsState.class);

    var driverPerformanceSerde = new JacksonJsonSerde<>(DriverPerformanceState.class);

    return input -> {

      var events = input.processValues(DeliveryEventDeduplicator::new, DeliveryEventDeduplicator.STORE);
      TimeAnalytics.build(events);
      KTable<String, DeliveryAnalyticsState> deliveries = events
          .groupByKey()
          .aggregate(
              DeliveryAnalyticsState::empty,
              (deliveryId, update, state) -> state.apply(update),
              Materialized
                  .<String, DeliveryAnalyticsState, KeyValueStore<Bytes, byte[]>>as(
                      "delivery-state-store")
                  .withKeySerde(Serdes.String())
                  .withValueSerde(deliveryStateSerde));

      deliveries.groupBy((id, state) -> KeyValue.pair("all", SummaryAggregate.contribution(state)),
          Grouped.with(Serdes.String(), new JacksonJsonSerde<>(SummaryAggregate.class)))
          .aggregate(SummaryAggregate::empty, (key, value, state) -> state.combine(value, 1),
              (key, value, state) -> state.combine(value, -1),
              Materialized.<String, SummaryAggregate, KeyValueStore<Bytes, byte[]>>as("delivery-summary-store")
                  .withKeySerde(Serdes.String()).withValueSerde(new JacksonJsonSerde<>(SummaryAggregate.class)));

      var driverDeliveries = deliveries.filter((id, delivery) -> delivery != null && delivery.driverId() != null);
      driverDeliveries.groupBy((id, delivery) -> KeyValue.pair(delivery.driverId().toString(), delivery),
          Grouped.with(Serdes.String(), deliveryStateSerde))
          .aggregate(() -> null,
              (id, delivery, state) -> (state == null ? DriverPerformanceState.empty(UUID.fromString(id)) : state).add(delivery),
              (id, delivery, state) -> state == null ? null : state.remove(delivery),
              Materialized.<String, DriverPerformanceState, KeyValueStore<Bytes, byte[]>>as("driver-performance-store")
                  .withKeySerde(Serdes.String()).withValueSerde(driverPerformanceSerde));
    };
  }
}
