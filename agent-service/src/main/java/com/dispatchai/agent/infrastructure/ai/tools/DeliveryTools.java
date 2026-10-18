package com.dispatchai.agent.infrastructure.ai.tools;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.dispatchai.agent.infrastructure.client.delivery.DeliveryClient;
import com.dispatchai.agent.infrastructure.client.delivery.dto.DeliveryResponse;

import dev.langchain4j.agent.tool.Tool;

@Component
public class DeliveryTools {

  private final DeliveryClient deliveryClient;

  public DeliveryTools(DeliveryClient deliveryClient) {
    this.deliveryClient = deliveryClient;
  }

  @Tool("""
      Get a delivery by ID.
      Returns the delivery's current status, assignedDriverId,
      addresses, and delay information.
      Use assignedDriverId with getDriver when driver details are required.
      """)
  public DeliveryResponse getDelivery(UUID deliveryId) {
    return deliveryClient.getDelivery(deliveryId);
  }
}
