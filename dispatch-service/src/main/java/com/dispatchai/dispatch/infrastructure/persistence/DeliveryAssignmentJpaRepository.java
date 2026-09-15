package com.dispatchai.dispatch.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;

public interface DeliveryAssignmentJpaRepository
    extends JpaRepository<DeliveryAssignment, UUID> {

  Optional<DeliveryAssignment> findByDeliveryId(UUID deliveryId);

  boolean existsByDeliveryIdAndStatus(
      UUID deliveryId,
      AssignmentStatus status);
}
