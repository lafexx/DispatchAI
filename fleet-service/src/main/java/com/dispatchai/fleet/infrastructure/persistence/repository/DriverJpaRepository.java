package com.dispatchai.fleet.infrastructure.persistence.repository;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.fleet.domain.model.Driver;

public interface DriverJpaRepository extends JpaRepository<Driver, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Driver r where r.id = :id")
  Optional<Driver> findByIdForUpdate(@Param("id") UUID id);
}
