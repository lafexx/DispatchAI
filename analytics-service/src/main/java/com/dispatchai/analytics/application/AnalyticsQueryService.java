package com.dispatchai.analytics.application;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Service;

import com.dispatchai.analytics.domain.model.AnalyticsDeliveryStatus;
import com.dispatchai.analytics.domain.model.DeliveryAnalyticsState;
import com.dispatchai.analytics.domain.model.DriverPerformanceState;
import com.dispatchai.analytics.presentation.dto.DeliverySummaryResponse;
import com.dispatchai.analytics.presentation.dto.DriverAnalyticsResponse;

@Service
public class AnalyticsQueryService {

  private final InteractiveQueryService interactiveQueryService;

  public AnalyticsQueryService(
      InteractiveQueryService interactiveQueryService) {
    this.interactiveQueryService = interactiveQueryService;
  }

  public DeliveryAnalyticsState getDelivery(UUID deliveryId) {
    var store = deliveryStateStore();

    return store.get(deliveryId.toString());
  }

  public List<DeliveryAnalyticsState> getAllDeliveries() {
    var store = deliveryStateStore();
    var deliveries = new ArrayList<DeliveryAnalyticsState>();

    try (var iterator = store.all()) {
      while (iterator.hasNext()) {
        deliveries.add(iterator.next().value);
      }
    }

    return deliveries;
  }

  public DeliverySummaryResponse getSummary() {
    ReadOnlyKeyValueStore<String, com.dispatchai.analytics.domain.model.SummaryAggregate> store =
        interactiveQueryService.getQueryableStore("delivery-summary-store", QueryableStoreTypes.keyValueStore());
    var summary = store.get("all");
    return com.dispatchai.analytics.application.mapper.DeliverySummaryMapper.toResponse(
        summary == null ? com.dispatchai.analytics.domain.model.SummaryAggregate.empty() : summary);
  }

  public List<com.dispatchai.analytics.presentation.dto.TimeBucketResponse> getHistory(String metric, int minutes) {
    if (minutes < 1 || minutes > 1440) throw new IllegalArgumentException("minutes must be between 1 and 1440");
    ReadOnlyKeyValueStore<String, com.dispatchai.analytics.domain.model.WindowActivity> store =
        interactiveQueryService.getQueryableStore(metric + "-history-store", QueryableStoreTypes.keyValueStore());
    long current = java.time.Instant.now().toEpochMilli() / 60000 * 60000;
    int size = metric.equals("delay") ? 5 : 1;
    var result = new ArrayList<com.dispatchai.analytics.presentation.dto.TimeBucketResponse>();
    // Delay points end at the last complete minute; throughput includes the open current minute.
    long lastStart = metric.equals("delay") ? current - size * 60000L : current;
    for (long start = lastStart - (minutes - 1L) * 60000; start <= lastStart; start += 60000) {
      var value = store.get(Long.toString(start));
      if (value == null) value = com.dispatchai.analytics.domain.model.WindowActivity.empty();
      result.add(new com.dispatchai.analytics.presentation.dto.TimeBucketResponse(
          java.time.Instant.ofEpochMilli(start), java.time.Instant.ofEpochMilli(start + size * 60000L),
          value.created(), value.completed(), value.active(), value.delayed(),
          value.active() == 0 ? null : (double)value.delayed()/value.active()));
    }
    return result;
  }

  public DriverAnalyticsResponse getDriverAnalytics(UUID driverId) {
    var state = driverPerformanceStore()
        .get(driverId.toString());

    if (state == null) {
      return new DriverAnalyticsResponse(
          driverId,
          0,
          0,
          0,
          0,
          0.0,
          0.0);
    }

    return new DriverAnalyticsResponse(
        state.driverId(),
        state.totalAssigned(),
        state.completed(),
        state.cancelled(),
        state.delayed(),
        state.averageDeliveryTimeSeconds(),
        state.averageTransitTimeSeconds());
  }

  private ReadOnlyKeyValueStore<String, DeliveryAnalyticsState> deliveryStateStore() {

    return interactiveQueryService.getQueryableStore(
        "delivery-state-store",
        QueryableStoreTypes.keyValueStore());
  }

  private ReadOnlyKeyValueStore<String, DriverPerformanceState> driverPerformanceStore() {

    return interactiveQueryService.getQueryableStore(
        "driver-performance-store",
        QueryableStoreTypes.keyValueStore());
  }
}
