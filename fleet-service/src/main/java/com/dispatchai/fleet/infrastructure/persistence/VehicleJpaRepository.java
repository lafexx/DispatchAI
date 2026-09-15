package com.dispatchai.fleet.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.fleet.domain.model.Vehicle;

public interface VehicleJpaRepository
    extends JpaRepository<Vehicle, UUID> {

  Optional<Vehicle> findByRegistration(String registration);
}
