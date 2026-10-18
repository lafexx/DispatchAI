package com.dispatchai.agent.infrastructure.client.fleet;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dispatchai.agent.infrastructure.client.fleet.dto.DriverResponse;

@Component
public class FleetClient {

  private final RestClient client;

  public FleetClient(
      RestClient.Builder builder,
      @Value("${services.fleet.base-url}") String baseUrl) {

    var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
        java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
    factory.setReadTimeout(java.time.Duration.ofSeconds(5));
    this.client = builder.requestFactory(factory)
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
