package com.dispatchai.agent.infrastructure.ai.tools;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.dispatchai.agent.infrastructure.client.FleetClient;
import com.dispatchai.agent.infrastructure.client.dto.DriverResponse;

import dev.langchain4j.agent.tool.Tool;

@Component
public class FleetTools {

  private final FleetClient fleetClient;

  public FleetTools(FleetClient fleetClient) {
    this.fleetClient = fleetClient;
  }

  @Tool("""
      Get a driver's current details and availability by driver ID.
      Use this after getDelivery when you need information about
      the delivery's assigned driver.
      """)
  public DriverResponse getDriver(UUID driverId) {
    return fleetClient.getDriver(driverId);
  }

  @Tool("""
      Get all drivers that are currently available for assignment
      """)
  public List<DriverResponse> getAvailableDrivers() {
    return fleetClient.getAvailableDrivers();
  }
}
