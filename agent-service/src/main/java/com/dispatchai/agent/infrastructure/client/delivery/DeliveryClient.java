package com.dispatchai.agent.infrastructure.client.delivery;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dispatchai.agent.infrastructure.client.delivery.dto.DeliveryResponse;

@Component
public class DeliveryClient {

  private final RestClient client;

  public DeliveryClient(
      RestClient.Builder builder,
      @Value("${services.delivery.base-url}") String baseUrl) {

    var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
        java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
    factory.setReadTimeout(java.time.Duration.ofSeconds(5));
    this.client = builder.requestFactory(factory)
        .baseUrl(baseUrl)
        .build();
  }

  public DeliveryResponse getDelivery(UUID deliveryId) {
    return client.get()
        .uri("/api/deliveries/{id}", deliveryId)
        .retrieve()
        .body(DeliveryResponse.class);
  }
}
