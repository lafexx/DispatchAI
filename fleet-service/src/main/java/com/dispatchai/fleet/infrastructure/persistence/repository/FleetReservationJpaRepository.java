package com.dispatchai.fleet.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dispatchai.fleet.domain.model.FleetReservation;
import com.dispatchai.fleet.domain.model.FleetReservationStatus;

public interface FleetReservationJpaRepository extends JpaRepository<FleetReservation, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from FleetReservation r where r.assignmentId = :id")
  Optional<FleetReservation> findByIdForUpdate(@Param("id") UUID id);

  List<FleetReservation> findByDeliveryIdAndStatusIn(UUID deliveryId, Collection<FleetReservationStatus> statuses);

  List<FleetReservation> findTop50ByStatusAndExpiresAtLessThanEqualOrderByExpiresAt(
      FleetReservationStatus status, Instant now);
}
