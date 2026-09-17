package com.dispatchai.agent.infrastructure.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dispatchai.agent.infrastructure.client.dto.DeliveryAnalyticsResponse;
import com.dispatchai.agent.infrastructure.client.dto.DeliverySummaryResponse;
import com.dispatchai.agent.infrastructure.client.dto.DriverAnalyticsResponse;

@Component
public class AnalyticsClient {

  private final RestClient client;

  public AnalyticsClient(
      RestClient.Builder builder,
      @Value("${services.analytics.base-url}") String baseUrl) {

    this.client = builder
        .baseUrl(baseUrl)
        .build();
  }

  public DeliverySummaryResponse getSummary() {
    return client.get()
        .uri("/api/analytics/deliveries/summary")
        .retrieve()
        .body(DeliverySummaryResponse.class);
  }

  public DeliveryAnalyticsResponse getDelivery(UUID deliveryId) {
    return client.get()
        .uri("/api/analytics/deliveries/{id}", deliveryId)
        .retrieve()
        .body(DeliveryAnalyticsResponse.class);
  }

  public DriverAnalyticsResponse getDriver(UUID driverId) {
    return client.get()
        .uri("/api/analytics/deliveries/drivers/{id}", driverId)
        .retrieve()
        .body(DriverAnalyticsResponse.class);
  }
}
