package com.dispatchai.agent.infrastructure.ai.tools;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.dispatchai.agent.domain.model.DispatchCandidate;
import com.dispatchai.agent.domain.model.DispatchRecommendationContext;
import com.dispatchai.agent.infrastructure.client.AnalyticsClient;
import com.dispatchai.agent.infrastructure.client.DeliveryClient;
import com.dispatchai.agent.infrastructure.client.FleetClient;

import dev.langchain4j.agent.tool.Tool;

@Component
public class DispatchTools {

  private final DeliveryClient deliveryClient;
  private final FleetClient fleetClient;
  private final AnalyticsClient analyticsClient;

  public DispatchTools(
      DeliveryClient deliveryClient,
      FleetClient fleetClient,
      AnalyticsClient analyticsClient) {
    this.deliveryClient = deliveryClient;
    this.fleetClient = fleetClient;
    this.analyticsClient = analyticsClient;
  }

  @Tool("""
      Get all live information needed to recommend a driver for a delivery.
      Returns the delivery, all currently available drivers, and operational
      analytics for each available driver.
      """)
  public DispatchRecommendationContext getDispatchRecommendationContext(
      UUID deliveryId) {

    var delivery = deliveryClient.getDelivery(deliveryId);

    var candidates = fleetClient.getAvailableDrivers().stream()
        .map(driver -> new DispatchCandidate(
            driver,
            analyticsClient.getDriver(driver.id())))
        .toList();

    return new DispatchRecommendationContext(
        delivery,
        candidates);
  }
}
