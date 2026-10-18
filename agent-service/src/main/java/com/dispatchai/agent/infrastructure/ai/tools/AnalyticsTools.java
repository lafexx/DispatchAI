package com.dispatchai.agent.infrastructure.ai.tools;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.dispatchai.agent.infrastructure.client.analytics.AnalyticsClient;
import com.dispatchai.agent.infrastructure.client.analytics.dto.DeliveryAnalyticsResponse;
import com.dispatchai.agent.infrastructure.client.analytics.dto.DeliverySummaryResponse;
import com.dispatchai.agent.infrastructure.client.analytics.dto.DriverAnalyticsResponse;

import dev.langchain4j.agent.tool.Tool;

@Component
public class AnalyticsTools {
  private final AnalyticsClient analyticsClient;

  public AnalyticsTools(AnalyticsClient analyticsClient) {
    this.analyticsClient = analyticsClient;
  }

  @Tool("Get the current overall delivery operations summary and performance metrics")
  public DeliverySummaryResponse getDeliverySummary() {
    System.out.println("Agent invoked getDeliverySummary()");
    return analyticsClient.getSummary();
  }

  @Tool("Get the current analytics state of a delivery using its delivery ID")
  public DeliveryAnalyticsResponse getDeliveryAnalytics(UUID deliveryId) {
    System.out.println("Agent invoked getDeliveryAnalytics()");
    return analyticsClient.getDelivery(deliveryId);
  }

  @Tool("Get operational analytics for a driver using their driver ID")
  public DriverAnalyticsResponse getDriverAnalytics(UUID driverId) {
    System.out.println("Agent invoked getDriverAnalytics()");
    return analyticsClient.getDriver(driverId);
  }
}
