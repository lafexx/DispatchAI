package com.dispatchai.agent.infrastructure.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dispatchai.agent.infrastructure.client.dto.DeliveryResponse;

@Component
public class DeliveryClient {

  private final RestClient client;

  public DeliveryClient(
      RestClient.Builder builder,
      @Value("${services.delivery.base-url}") String baseUrl) {

    this.client = builder
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
