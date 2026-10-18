
package com.dispatchai.dispatch.application;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.dispatchai.dispatch.domain.model.ReservationCleanup;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.persistence.repository.ReservationCleanupJpaRepository;

@Service
public class ReservationCleanupService {
  private static final Logger log = LoggerFactory.getLogger(ReservationCleanupService.class);
  private final FleetClient fleet;
  private final ReservationCleanupJpaRepository repository;

  public ReservationCleanupService(FleetClient fleet, ReservationCleanupJpaRepository repository) {
    this.fleet = fleet;
    this.repository = repository;
  }

  @org.springframework.transaction.annotation.Transactional(propagation =
      org.springframework.transaction.annotation.Propagation.MANDATORY)
  public void enqueue(UUID id, ReservationRequest request) {
    repository.saveAndFlush(new ReservationCleanup(id, request.deliveryId(), request.driverId(), request.vehicleId()));
  }

  public void compensate(UUID id, ReservationRequest request) {
    // The orchestration calls this only after its local transaction has ended.
    // Persist intent before attempting release; transport failure retains it.
    repository.saveAndFlush(new ReservationCleanup(id, request.deliveryId(), request.driverId(), request.vehicleId()));
    retry(id, request);
  }

  public void retry(UUID id, ReservationRequest request) {
    try {
      fleet.release(id, request);
      repository.deleteById(id);
    } catch (RuntimeException ex) {
      log.warn("Reservation compensation remains pending assignment={}", id, ex);
    }
  }
}
