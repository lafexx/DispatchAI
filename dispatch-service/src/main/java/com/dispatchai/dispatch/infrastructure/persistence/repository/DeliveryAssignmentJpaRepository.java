package com.dispatchai.dispatch.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;

public interface DeliveryAssignmentJpaRepository extends JpaRepository<DeliveryAssignment, UUID> {
  Optional<DeliveryAssignment> findFirstByDeliveryIdAndStatusOrderByAssignedAtDesc(UUID deliveryId,
      AssignmentStatus status);

  Optional<DeliveryAssignment> findFirstByDeliveryIdOrderByAssignedAtDescIdDesc(UUID deliveryId);

  default Optional<DeliveryAssignment> findByDeliveryId(UUID deliveryId) {
    var active = findFirstByDeliveryIdAndStatusOrderByAssignedAtDesc(deliveryId, AssignmentStatus.ACTIVE);
    return active.isPresent() ? active : findFirstByDeliveryIdOrderByAssignedAtDescIdDesc(deliveryId);
  }

  List<DeliveryAssignment> findByDeliveryIdAndStatus(UUID deliveryId, AssignmentStatus status);

  List<DeliveryAssignment> findByDriverId(UUID driverId);

  List<DeliveryAssignment> findByDriverIdAndStatus(UUID driverId, AssignmentStatus status);

  boolean existsByDeliveryIdAndStatus(UUID deliveryId, AssignmentStatus status);

  List<DeliveryAssignment> findByStatus(AssignmentStatus status);
}
