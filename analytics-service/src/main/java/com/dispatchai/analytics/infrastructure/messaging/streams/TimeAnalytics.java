package com.dispatchai.analytics.infrastructure.messaging.streams;

import java.time.Duration;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.*;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import com.dispatchai.analytics.domain.model.DeliveryStateUpdate;
import com.dispatchai.analytics.domain.model.WindowActivity;

public final class TimeAnalytics {
  private TimeAnalytics() {}
  public static void build(KStream<String, DeliveryStateUpdate> events) {
    buildWindow(events, "throughput", Duration.ofMinutes(1));
    buildWindow(events, "delay", Duration.ofMinutes(5));
  }
  private static void buildWindow(KStream<String, DeliveryStateUpdate> events, String name, Duration size) {
    var serde = new JacksonJsonSerde<>(WindowActivity.class);
    // Per-delivery aggregation means retries and multiple delay reports do not inflate counts.
    var windows = events.groupByKey(Grouped.with(Serdes.String(), new JacksonJsonSerde<>(DeliveryStateUpdate.class)))
        .windowedBy(TimeWindows.ofSizeAndGrace(size, Duration.ofMinutes(2)).advanceBy(Duration.ofMinutes(1)))
        .aggregate(WindowActivity::empty, (key, update, state) -> state.apply(update),
            Materialized.<String, WindowActivity, WindowStore<Bytes, byte[]>>as(name + "-delivery-windows")
                .withKeySerde(Serdes.String()).withValueSerde(serde).withRetention(Duration.ofHours(24)));
    windows.groupBy((key, value) -> KeyValue.pair(Long.toString(key.window().start()), value),
        Grouped.with(Serdes.String(), serde))
        .aggregate(WindowActivity::empty, (key, value, state) -> state.plus(value),
            (key, value, state) -> state.minus(value),
            Materialized.<String, WindowActivity, KeyValueStore<Bytes, byte[]>>as(name + "-history-store")
                .withKeySerde(Serdes.String()).withValueSerde(serde));
  }
}
