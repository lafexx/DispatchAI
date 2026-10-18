package com.dispatchai.customer.domain.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class Customer {

  @Id
  private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  private String phone;

  protected Customer() {
  }

  public Customer(
      UUID id,
      String name,
      String email,
      String phone) {

    if (id == null) {
      throw new IllegalArgumentException("Customer ID cannot be null");
    }

    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Customer name cannot be blank");
    }

    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Customer email cannot be blank");
    }

    this.id = id;
    this.name = name;
    this.email = email;
    this.phone = phone;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
  }
}
