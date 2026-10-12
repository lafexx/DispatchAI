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
