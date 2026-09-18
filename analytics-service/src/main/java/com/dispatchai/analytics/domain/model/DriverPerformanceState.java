package com.dispatchai.analytics.domain.model;

import java.time.Duration;
import java.util.UUID;

public record DriverPerformanceState(
    UUID driverId,
    long totalAssigned,
    long active,
    long completed,
    long cancelled,
    long delayed,
    long completedTransitCount,
    long totalTransitTimeMillis,
    long completedDeliveryCount,
    long totalDeliveryTimeMillis) {

  public static DriverPerformanceState empty(UUID driverId) {
    return new DriverPerformanceState(
        driverId,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0,
        0);
  }

  public DriverPerformanceState add(DeliveryAnalyticsState delivery) {
    long transitMillis = transitTimeMillis(delivery);
    long deliveryMillis = deliveryTimeMillis(delivery);

    return new DriverPerformanceState(
        driverId,
        totalAssigned + 1,
        active + (isActive(delivery) ? 1 : 0),
        completed + (isCompleted(delivery) ? 1 : 0),
        cancelled + (isCancelled(delivery) ? 1 : 0),
        delayed + (delivery.delayed() ? 1 : 0),
        completedTransitCount + (transitMillis >= 0 ? 1 : 0),
        totalTransitTimeMillis + Math.max(transitMillis, 0),
        completedDeliveryCount + (deliveryMillis >= 0 ? 1 : 0),
        totalDeliveryTimeMillis + Math.max(deliveryMillis, 0));
  }

  public DriverPerformanceState remove(DeliveryAnalyticsState delivery) {
    long transitMillis = transitTimeMillis(delivery);
    long deliveryMillis = deliveryTimeMillis(delivery);

    return new DriverPerformanceState(
        driverId,
        totalAssigned - 1,
        active - (isActive(delivery) ? 1 : 0),
        completed - (isCompleted(delivery) ? 1 : 0),
        cancelled - (isCancelled(delivery) ? 1 : 0),
        delayed - (delivery.delayed() ? 1 : 0),
        completedTransitCount - (transitMillis >= 0 ? 1 : 0),
        totalTransitTimeMillis - Math.max(transitMillis, 0),
        completedDeliveryCount - (deliveryMillis >= 0 ? 1 : 0),
        totalDeliveryTimeMillis - Math.max(deliveryMillis, 0));
  }

  private static boolean isActive(DeliveryAnalyticsState delivery) {
    return delivery.status() == AnalyticsDeliveryStatus.ASSIGNED
        || delivery.status() == AnalyticsDeliveryStatus.IN_TRANSIT;
  }

  private static boolean isCompleted(DeliveryAnalyticsState delivery) {
    return delivery.status() == AnalyticsDeliveryStatus.COMPLETED;
  }

  private static boolean isCancelled(DeliveryAnalyticsState delivery) {
    return delivery.status() == AnalyticsDeliveryStatus.CANCELLED;
  }

  private static long transitTimeMillis(DeliveryAnalyticsState delivery) {
    if (delivery.startedAt() == null || delivery.completedAt() == null) {
      return -1;
    }

    return Duration.between(
        delivery.startedAt(),
        delivery.completedAt()).toMillis();
  }

  private static long deliveryTimeMillis(DeliveryAnalyticsState delivery) {
    if (delivery.createdAt() == null || delivery.completedAt() == null) {
      return -1;
    }

    return Duration.between(
        delivery.createdAt(),
        delivery.completedAt()).toMillis();
  }

  public double averageTransitTimeSeconds() {
    if (completedTransitCount == 0) {
      return 0.0;
    }

    return (totalTransitTimeMillis / (double) completedTransitCount) / 1000.0;
  }

  public double averageDeliveryTimeSeconds() {
    if (completedDeliveryCount == 0) {
      return 0.0;
    }

    return (totalDeliveryTimeMillis / (double) completedDeliveryCount) / 1000.0;
  }
}
