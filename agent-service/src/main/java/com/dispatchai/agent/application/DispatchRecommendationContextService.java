package com.dispatchai.agent.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.agent.domain.model.DispatchCandidate;
import com.dispatchai.agent.domain.model.DispatchRecommendationContext;
import com.dispatchai.agent.infrastructure.client.AnalyticsClient;
import com.dispatchai.agent.infrastructure.client.DeliveryClient;
import com.dispatchai.agent.infrastructure.client.FleetClient;

@Service
public class DispatchRecommendationContextService {

  private final DeliveryClient deliveryClient;
  private final FleetClient fleetClient;
  private final AnalyticsClient analyticsClient;

  public DispatchRecommendationContextService(
      DeliveryClient deliveryClient,
      FleetClient fleetClient,
      AnalyticsClient analyticsClient) {
    this.deliveryClient = deliveryClient;
    this.fleetClient = fleetClient;
    this.analyticsClient = analyticsClient;
  }

  public DispatchRecommendationContext get(UUID deliveryId) {
    var delivery = deliveryClient.getDelivery(deliveryId);

    var drivers = fleetClient.getAvailableDrivers();

    var candidates = drivers.stream()
        .map(driver -> new DispatchCandidate(
            driver,
            analyticsClient.getDriver(
                driver.id())))
        .toList();

    return new DispatchRecommendationContext(
        delivery,
        candidates);
  }
}
