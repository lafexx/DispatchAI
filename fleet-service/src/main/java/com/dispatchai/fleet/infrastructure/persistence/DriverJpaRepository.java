package com.dispatchai.fleet.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.fleet.domain.model.Driver;

public interface DriverJpaRepository extends JpaRepository<Driver, UUID> {
}
