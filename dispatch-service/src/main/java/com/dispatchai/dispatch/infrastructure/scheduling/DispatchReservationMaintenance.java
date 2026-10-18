
package com.dispatchai.dispatch.infrastructure.scheduling;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.dispatchai.dispatch.application.DispatchApplicationService;
import com.dispatchai.dispatch.application.ReservationCleanupService;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.repository.ReservationCleanupJpaRepository;

@Component
public class DispatchReservationMaintenance {
  private static final Logger log = LoggerFactory.getLogger(DispatchReservationMaintenance.class);
  private final DeliveryAssignmentJpaRepository assignments;
  private final ReservationCleanupJpaRepository cleanups;
  private final DispatchApplicationService service;
  private final ReservationCleanupService cleanup;
  private final FleetClient fleet;
  private final com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient deliveries;

  public DispatchReservationMaintenance(DeliveryAssignmentJpaRepository assignments,
      ReservationCleanupJpaRepository cleanups,
      DispatchApplicationService service, ReservationCleanupService cleanup, FleetClient fleet,
      com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient deliveries) {
    this.assignments = assignments;
    this.cleanups = cleanups;
    this.service = service;
    this.cleanup = cleanup;
    this.fleet = fleet;
    this.deliveries = deliveries;
  }

  @Scheduled(fixedDelayString = "${dispatch.reservation.sweep-ms:5000}")
  public void maintain() {
    for (var pending : cleanups.findAll()) {
      cleanup.retry(pending.getAssignmentId(),
          new ReservationRequest(pending.getDeliveryId(), pending.getDriverId(), pending.getVehicleId()));
    }
    for (var assignment : assignments.findByStatus(AssignmentStatus.ACTIVE)) {
      try {
        var reservation = fleet.reservation(assignment.getId());
        if (reservation != null && "RELEASED".equals(reservation.status())
            && !"TERMINAL_DELIVERY".equals(reservation.releaseReason())) {
          var delivery = deliveries.getDelivery(assignment.getDeliveryId());
          if (delivery.status() == com.dispatchai.dispatch.infrastructure.client.delivery.dto.RemoteDeliveryStatus.PENDING) {
            service.rejectAssignment(assignment.getId(), assignment.getDeliveryId(),
                "Reservation was released or expired", Instant.now());
          } // Otherwise wait for the authoritative accepted/terminal outcome, never guess
            // from elapsed time.
        }
      } catch (RuntimeException ex) {
        log.warn("Assignment reconciliation pending id={}", assignment.getId(), ex);
      }
    }
  }
}
