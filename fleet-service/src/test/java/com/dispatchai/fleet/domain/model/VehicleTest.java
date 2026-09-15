package com.dispatchai.fleet.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class VehicleTest {
  private Vehicle createVehicle() {
    return new Vehicle(
        UUID.randomUUID(),
        "ABC123",
        VehicleType.VAN,
        new BigDecimal("1200.00"));
  }

  @Test
  void newVehicleStartsUnavailable() {
    var vehicle = createVehicle();

    assertFalse(vehicle.isAvailable());
  }

  @Test
  void vehicleCanBeMarkedAvailable() {
    var vehicle = createVehicle();

    vehicle.markAvailable();

    assertTrue(vehicle.isAvailable());
  }

  @Test
  void availableVehicleCanBeMarkedUnavailable() {
    var vehicle = createVehicle();

    vehicle.markAvailable();
    vehicle.markUnavailable();

    assertFalse(vehicle.isAvailable());
  }

  @Test
  void vehicleCapacityMustBePositive() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Vehicle(
            UUID.randomUUID(),
            "ABC123",
            VehicleType.VAN,
            BigDecimal.ZERO));
  }
}
