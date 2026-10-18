package com.dispatchai.fleet.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dispatchai.fleet.domain.model.FleetAssignment;

public interface FleetAssignmentJpaRepository extends JpaRepository<FleetAssignment, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from FleetAssignment a where a.deliveryId = :deliveryId")
  Optional<FleetAssignment> findByDeliveryIdForUpdate(@Param("deliveryId") UUID deliveryId);
}
