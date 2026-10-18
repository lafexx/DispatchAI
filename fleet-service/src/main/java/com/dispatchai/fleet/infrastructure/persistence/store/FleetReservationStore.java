package com.dispatchai.fleet.infrastructure.persistence.store;

import com.dispatchai.fleet.infrastructure.persistence.repository.FleetReservationJpaRepository;

import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.dispatchai.fleet.domain.model.FleetReservation;

/**
 * Establishes a durable row to lock, including for terminal-before-assignment.
 */
@Component
public class FleetReservationStore {
  private final FleetReservationJpaRepository repository;
  private final TransactionTemplate initialization;

  public FleetReservationStore(FleetReservationJpaRepository repository, PlatformTransactionManager manager) {
    this.repository = repository;
    initialization = new TransactionTemplate(manager);
    initialization.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public FleetReservation lock(UUID assignmentId) {
    Objects.requireNonNull(assignmentId, "Assignment ID is required");
    // Initialize independently: a simultaneous first event may win the insert.
    // Catch outside that transaction so a unique-key loser does not poison the
    // caller's transaction. Every state change then shares the same row lock.
    try {
      initialization.executeWithoutResult(tx -> {
        if (!repository.existsById(assignmentId)) {
          repository.saveAndFlush(new FleetReservation(assignmentId));
        }
      });
    } catch (DataIntegrityViolationException concurrentInsert) {
      if (!repository.existsById(assignmentId)) {
        throw concurrentInsert;
      }
    }
    return repository.findByIdForUpdate(assignmentId)
        .orElseThrow(() -> new IllegalStateException("Lifecycle row is missing"));
  }
}
