package com.dispatchai.agent.infrastructure.client.analytics;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dispatchai.agent.infrastructure.client.analytics.dto.DeliveryAnalyticsResponse;
import com.dispatchai.agent.infrastructure.client.analytics.dto.DeliverySummaryResponse;
import com.dispatchai.agent.infrastructure.client.analytics.dto.DriverAnalyticsResponse;

@Component
public class AnalyticsClient {

  private final RestClient client;

  public AnalyticsClient(
      RestClient.Builder builder,
      @Value("${services.analytics.base-url}") String baseUrl) {

    var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
        java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
    factory.setReadTimeout(java.time.Duration.ofSeconds(5));
    this.client = builder.requestFactory(factory)
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
