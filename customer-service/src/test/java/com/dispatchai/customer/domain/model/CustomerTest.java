package com.dispatchai.customer.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CustomerTest {

  @Test
  void customerIsCreatedWithExpectedValues() {
    UUID id = UUID.randomUUID();

    Customer customer = new Customer(
        id,
        "Alex Smith",
        "alex@example.com",
        "0400000000");

    assertEquals(id, customer.getId());
    assertEquals("Alex Smith", customer.getName());
    assertEquals("alex@example.com", customer.getEmail());
    assertEquals("0400000000", customer.getPhone());
  }

  @Test
  void phoneCanBeNull() {
    UUID id = UUID.randomUUID();

    Customer customer = new Customer(
        id,
        "Alex Smith",
        "alex@example.com",
        null);

    assertNull(customer.getPhone());
  }

  @Test
  void customerIdCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Customer(
            null,
            "Alex Smith",
            "alex@example.com",
            "0400000000"));

    assertEquals(
        "Customer ID cannot be null",
        exception.getMessage());
  }

  @Test
  void customerNameCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Customer(
            UUID.randomUUID(),
            null,
            "alex@example.com",
            "0400000000"));

    assertEquals(
        "Customer name cannot be blank",
        exception.getMessage());
  }

  @Test
  void customerNameCannotBeBlank() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Customer(
            UUID.randomUUID(),
            "   ",
            "alex@example.com",
            "0400000000"));

    assertEquals(
        "Customer name cannot be blank",
        exception.getMessage());
  }

  @Test
  void customerEmailCannotBeNull() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Customer(
            UUID.randomUUID(),
            "Alex Smith",
            null,
            "0400000000"));

    assertEquals(
        "Customer email cannot be blank",
        exception.getMessage());
  }

  @Test
  void customerEmailCannotBeBlank() {
    IllegalArgumentException exception = assertThrows(
        IllegalArgumentException.class,
        () -> new Customer(
            UUID.randomUUID(),
            "Alex Smith",
            "   ",
            "0400000000"));

    assertEquals(
        "Customer email cannot be blank",
        exception.getMessage());
  }
}
