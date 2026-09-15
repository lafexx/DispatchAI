package com.dispatchai.dispatch.infrastructure.delivery;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotFoundException;

@Component
public class DeliveryClient {
  private final RestClient restClient;

  public DeliveryClient(
      RestClient.Builder builder,
      @Value("${delivery-service.base-url}") String deliveryBaseUrl) {

    this.restClient = builder
        .baseUrl(deliveryBaseUrl)
        .build();
  }

  public DeliveryResponse getDelivery(UUID deliveryId) {
    try {

      return restClient
          .get()
          .uri("/api/deliveries/{id}", deliveryId)
          .retrieve()
          .body(DeliveryResponse.class);
    } catch (HttpClientErrorException.NotFound ex) {
      throw new DeliveryNotFoundException(deliveryId);
    }
  }
}
