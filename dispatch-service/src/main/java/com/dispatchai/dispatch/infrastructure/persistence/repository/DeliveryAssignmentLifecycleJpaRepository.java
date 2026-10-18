package com.dispatchai.dispatch.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dispatchai.dispatch.domain.model.DeliveryAssignmentLifecycle;

public interface DeliveryAssignmentLifecycleJpaRepository extends JpaRepository<DeliveryAssignmentLifecycle, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from DeliveryAssignmentLifecycle a where a.deliveryId = :deliveryId")
  Optional<DeliveryAssignmentLifecycle> findByDeliveryIdForUpdate(@Param("deliveryId") UUID deliveryId);
}
