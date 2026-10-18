package com.dispatchai.dispatch.infrastructure.client.fleet;

import com.dispatchai.dispatch.infrastructure.client.fleet.dto.DriverResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationResponse;

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
    var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(
        java.net.http.HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(3)).build());
    factory.setReadTimeout(java.time.Duration.ofSeconds(5));
    this.restClient = builder.baseUrl(fleetBaseUrl).requestFactory(factory).build();
  }

  public ReservationResponse reserve(UUID assignmentId, ReservationRequest request) {
    try {
      return restClient.put().uri("/api/reservations/{id}", assignmentId).body(request)
          .retrieve().body(ReservationResponse.class);
    } catch (org.springframework.web.client.HttpClientErrorException.Conflict ex) {
      throw new com.dispatchai.dispatch.domain.exception.ReservationConflictException(
          ex.getResponseBodyAsString());
    }
  }

  public ReservationResponse reservation(UUID assignmentId) {
    return restClient.get().uri("/api/reservations/{id}", assignmentId).retrieve().body(ReservationResponse.class);
  }

  public void release(UUID assignmentId, ReservationRequest request) {
    restClient.post().uri("/api/reservations/{id}/release", assignmentId).body(request)
        .retrieve().toBodilessEntity();
  }

  public List<DriverResponse> getAvailableDrivers() {
    return restClient
        .get()
        .uri("/api/drivers/available")
        .retrieve()
        .body(new ParameterizedTypeReference<List<DriverResponse>>() {
        });
  }

  public List<VehicleResponse> getVehicles() {
    return restClient.get().uri("/api/vehicles").retrieve()
        .body(new ParameterizedTypeReference<List<VehicleResponse>>() {});
  }

  public VehicleResponse getVehicle(UUID vehicleId) {
    return restClient.get().uri("/api/vehicles/{id}", vehicleId)
        .retrieve().body(VehicleResponse.class);
  }

  public DriverResponse getDriver(UUID driverId) {
    return restClient
        .get()
        .uri("/api/drivers/{id}", driverId)
        .retrieve()
        .body(DriverResponse.class);
  }
}
