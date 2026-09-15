package com.dispatchai.dispatch.infrastructure.fleet;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class FleetClient {
  private final RestClient restClient;

  public FleetClient(RestClient.Builder builder, @Value("${fleet-service.base-url}") String fleetBaseUrl) {
    this.restClient = builder.baseUrl(fleetBaseUrl).build();
  }

  public List<FleetDriverResponse> getAvailableDrivers() {
    return restClient
        .get()
        .uri("/api/drivers/available")
        .retrieve()
        .body(new ParameterizedTypeReference<List<FleetDriverResponse>>() {
        });
  }

  public FleetDriverResponse getDriver(UUID driverId) {
    return restClient
        .get()
        .uri("/api/drivers/{id}", driverId)
        .retrieve()
        .body(FleetDriverResponse.class);
  }
}
