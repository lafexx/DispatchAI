package com.dispatchai.customer.infrastructure.persistence.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.customer.domain.model.Customer;

public interface CustomerJpaRepository extends JpaRepository<Customer, UUID> {
}
