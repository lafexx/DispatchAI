package com.dispatchai.analytics.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dispatchai.analytics.application.AnalyticsQueryService;
import com.dispatchai.analytics.domain.model.DeliveryAnalyticsState;
import com.dispatchai.analytics.presentation.dto.DeliverySummaryResponse;
import com.dispatchai.analytics.presentation.dto.DriverAnalyticsResponse;

@RestController
@RequestMapping("/api/analytics/deliveries")
public class AnalyticsController {

  private final AnalyticsQueryService queryService;

  public AnalyticsController(AnalyticsQueryService queryService) {
    this.queryService = queryService;
  }

  @GetMapping("/throughput")
  public List<com.dispatchai.analytics.presentation.dto.TimeBucketResponse> throughput(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue="60") int minutes) {
    return history("throughput", minutes);
  }
  @GetMapping("/delay-rate")
  public List<com.dispatchai.analytics.presentation.dto.TimeBucketResponse> delayRate(
      @org.springframework.web.bind.annotation.RequestParam(defaultValue="60") int minutes) {
    return history("delay", minutes);
  }
  private List<com.dispatchai.analytics.presentation.dto.TimeBucketResponse> history(String metric, int minutes) {
    if (minutes < 1 || minutes > 1440) throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST, "minutes must be between 1 and 1440");
    try { return queryService.getHistory(metric, minutes); }
    catch (org.apache.kafka.streams.errors.InvalidStateStoreException e) {
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
          "Analytics stores are restoring; retry shortly", e);
    }
  }

  @GetMapping("/summary")
  public DeliverySummaryResponse summary() {
    return queryService.getSummary();
  }

  @GetMapping("/{deliveryId}")
  public ResponseEntity<DeliveryAnalyticsState> getDelivery(
      @PathVariable UUID deliveryId) {

    var delivery = queryService.getDelivery(deliveryId);

    if (delivery == null) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(delivery);
  }

  @GetMapping("/drivers/{driverId}")
  public DriverAnalyticsResponse getDriverAnalytics(
      @PathVariable UUID driverId) {

    return queryService.getDriverAnalytics(driverId);
  }

  @GetMapping
  public List<DeliveryAnalyticsState> getDeliveries() {
    return queryService.getAllDeliveries();
  }
}
