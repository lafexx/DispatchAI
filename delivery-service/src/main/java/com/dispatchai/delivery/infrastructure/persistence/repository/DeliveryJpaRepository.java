package com.dispatchai.delivery.infrastructure.persistence.repository;

import com.dispatchai.delivery.domain.model.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryJpaRepository extends JpaRepository<Delivery, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Delivery r where r.id = :id")
  Optional<Delivery> findByIdForUpdate(@Param("id") UUID id);
}
