package com.dispatchai.agent.infrastructure.client;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dispatchai.agent.infrastructure.client.dto.DriverResponse;

@Component
public class FleetClient {

  private final RestClient client;

  public FleetClient(
      RestClient.Builder builder,
      @Value("${services.fleet.base-url}") String baseUrl) {

    this.client = builder
        .baseUrl(baseUrl)
        .build();
  }

  public DriverResponse getDriver(UUID driverId) {
    return client.get()
        .uri("/api/drivers/{id}", driverId)
        .retrieve()
        .body(DriverResponse.class);
  }

  public List<DriverResponse> getAvailableDrivers() {
    return client.get()
        .uri("/api/drivers/available")
        .retrieve()
        .body(new ParameterizedTypeReference<List<DriverResponse>>() {
        });
  }
}
