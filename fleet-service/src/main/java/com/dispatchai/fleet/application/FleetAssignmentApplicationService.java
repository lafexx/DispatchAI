package com.dispatchai.fleet.application;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dispatchai.fleet.domain.model.FleetAssignmentStatus;
import com.dispatchai.fleet.infrastructure.persistence.store.FleetAssignmentStore;

@Service
public class FleetAssignmentApplicationService {
  private static final Logger log = LoggerFactory.getLogger(FleetAssignmentApplicationService.class);
  private final FleetAssignmentStore assignments;
  private final FleetReservationService reservations;
  public FleetAssignmentApplicationService(FleetAssignmentStore assignments, FleetReservationService reservations) {
    this.assignments = assignments; this.reservations = reservations;
  }

  @Transactional
  public void assigned(UUID deliveryId, UUID assignmentId, UUID driverId, UUID vehicleId, Instant occurredAt) {
    var assignment = assignments.lock(deliveryId);
    var reservation = reservations.get(assignmentId);
    if (!deliveryId.equals(reservation.deliveryId()) || !driverId.equals(reservation.driverId())
        || !Objects.equals(vehicleId, reservation.vehicleId())) {
      throw new IllegalStateException("Accepted assignment does not match reservation");
    }
    if (reservation.status() != com.dispatchai.fleet.domain.model.FleetReservationStatus.CONFIRMED) { return; }
    assignment.recordAssignment(assignmentId, driverId, vehicleId, occurredAt);
    // Reservation already owns occupancy. Accepted event must not increment again.
    log.info("Accepted assignment projection delivery={} assignment={}", deliveryId, assignmentId);
  }

  @Transactional
  public void closed(UUID deliveryId, FleetAssignmentStatus terminal, Instant occurredAt) {
    var assignment = assignments.lock(deliveryId);
    assignment.recordTerminal(terminal, occurredAt);
    reservations.closeForDelivery(deliveryId); // Includes holds not yet represented by an accepted event.
  }
}

