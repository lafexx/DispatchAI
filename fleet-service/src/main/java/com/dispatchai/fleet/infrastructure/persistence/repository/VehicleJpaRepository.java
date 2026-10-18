package com.dispatchai.fleet.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.fleet.domain.model.Vehicle;

public interface VehicleJpaRepository
    extends JpaRepository<Vehicle, UUID> {

  Optional<Vehicle> findByRegistration(String registration);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Vehicle r where r.id = :id")
  Optional<Vehicle> findByIdForUpdate(@Param("id") UUID id);
}
