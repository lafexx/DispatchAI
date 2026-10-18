package com.dispatchai.delivery.infrastructure.client.fleet;

import com.dispatchai.delivery.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.delivery.infrastructure.client.fleet.dto.ReservationResponse;

import com.dispatchai.delivery.infrastructure.client.fleet.exception.ReservationDeclinedException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Uses JDK HTTP so Delivery needs no additional starter dependency. */
@Component
public class FleetReservationClient {
  private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final String baseUrl;
  private final ObjectMapper mapper;

  public FleetReservationClient(ObjectMapper mapper,
      @Value("${fleet-service.base-url:http://localhost:8082}") String baseUrl) {
    this.mapper = mapper;
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public void confirm(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId) {
    confirm(assignmentId, deliveryId, driverId, vehicleId, null);
  }

  public void confirm(UUID assignmentId, UUID deliveryId, UUID driverId, UUID vehicleId, java.math.BigDecimal weight) {
    var body = mapper.writeValueAsString(new ReservationRequest(deliveryId, driverId, vehicleId, weight));
    var request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/reservations/" + assignmentId + "/confirm"))
        .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    try {
      var response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 404 || response.statusCode() == 409) {
        throw new ReservationDeclinedException("Reservation is no longer eligible");
      }
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("Fleet confirmation failed HTTP " + response.statusCode());
      }
      var reservation = mapper.readValue(response.body(), ReservationResponse.class);
      if (!assignmentId.equals(reservation.assignmentId()) || !deliveryId.equals(reservation.deliveryId())
          || !driverId.equals(reservation.driverId()) || !Objects.equals(vehicleId, reservation.vehicleId())
          || !"CONFIRMED".equals(reservation.status())) {
        throw new IllegalStateException("Fleet returned a mismatched confirmation");
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Fleet confirmation interrupted", ex);
    } catch (IOException ex) {
      throw new IllegalStateException("Fleet confirmation transport failed", ex);
    }
  }

}
