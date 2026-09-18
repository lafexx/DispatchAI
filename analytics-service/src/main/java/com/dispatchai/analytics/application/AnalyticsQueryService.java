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
    var deliveries = getAllDeliveries();

    long total = deliveries.size();

    long pending = deliveries.stream()
        .filter(d -> d.status() == AnalyticsDeliveryStatus.PENDING)
        .count();

    long assigned = deliveries.stream()
        .filter(d -> d.status() == AnalyticsDeliveryStatus.ASSIGNED)
        .count();

    long inTransit = deliveries.stream()
        .filter(d -> d.status() == AnalyticsDeliveryStatus.IN_TRANSIT)
        .count();

    long completed = deliveries.stream()
        .filter(d -> d.status() == AnalyticsDeliveryStatus.COMPLETED)
        .count();

    long cancelled = deliveries.stream()
        .filter(d -> d.status() == AnalyticsDeliveryStatus.CANCELLED)
        .count();

    long delayed = deliveries.stream()
        .filter(DeliveryAnalyticsState::delayed)
        .count();

    double averageDelayMinutes = deliveries.stream()
        .filter(d -> d.delayMinutes() != null)
        .mapToInt(DeliveryAnalyticsState::delayMinutes)
        .average()
        .orElse(0.0);

    double averageAssignmentTimeSeconds = deliveries.stream()
        .filter(d -> d.createdAt() != null && d.assignedAt() != null)
        .mapToLong(d -> Duration.between(
            d.createdAt(),
            d.assignedAt()).toMillis())
        .average()
        .orElse(0.0) / 1000.0;

    double averageDeliveryTimeSeconds = deliveries.stream()
        .filter(d -> d.createdAt() != null && d.completedAt() != null)
        .mapToLong(d -> Duration.between(
            d.createdAt(),
            d.completedAt()).toMillis())
        .average()
        .orElse(0.0) / 1000.0;

    double averageTransitTimeSeconds = deliveries.stream()
        .filter(d -> d.startedAt() != null && d.completedAt() != null)
        .mapToLong(d -> Duration.between(
            d.startedAt(),
            d.completedAt()).toMillis())
        .average()
        .orElse(0.0) / 1000.0;

    double completionRate = total == 0 ? 0.0 : (double) completed / total;
    double cancellationRate = total == 0 ? 0.0 : (double) cancelled / total;
    double delayRate = total == 0 ? 0.0 : (double) delayed / total;

    return new DeliverySummaryResponse(
        total,
        pending,
        assigned,
        inTransit,
        completed,
        cancelled,
        delayed,
        averageDelayMinutes,
        averageAssignmentTimeSeconds,
        averageDeliveryTimeSeconds,
        averageTransitTimeSeconds,
        completionRate,
        cancellationRate,
        delayRate);
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
