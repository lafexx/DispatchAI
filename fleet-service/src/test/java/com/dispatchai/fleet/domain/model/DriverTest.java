package com.dispatchai.fleet.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class DriverTest {

  private Driver createDriver() {
    return new Driver(
        UUID.randomUUID(),
        "Alex Smith");
  }

  @Test
  void newDriverStartsUnavailable() {
    var driver = createDriver();

    assertEquals(
        DriverAvailability.UNAVAILABLE,
        driver.getAvailability());

    assertFalse(driver.isAvailable());
  }

  @Test
  void driverCanBeMarkedAvailable() {
    var driver = createDriver();

    driver.markAvailable();

    assertEquals(
        DriverAvailability.AVAILABLE,
        driver.getAvailability());

    assertTrue(driver.isAvailable());
  }

  @Test
  void availableDriverCanBeMarkedUnavailable() {
    var driver = createDriver();

    driver.markAvailable();
    driver.markUnavailable();

    assertEquals(
        DriverAvailability.UNAVAILABLE,
        driver.getAvailability());

    assertFalse(driver.isAvailable());
  }

  @Test
  void driverIdCannotBeNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Driver(
            null,
            "Alex Smith"));
  }

  @Test
  void driverNameCannotBeNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Driver(
            UUID.randomUUID(),
            null));
  }

  @Test
  void driverNameCannotBeBlank() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Driver(
            UUID.randomUUID(),
            "   "));
  }
}
