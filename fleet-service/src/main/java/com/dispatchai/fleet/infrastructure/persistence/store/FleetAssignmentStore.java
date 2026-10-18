package com.dispatchai.fleet.infrastructure.persistence.store;

import com.dispatchai.fleet.infrastructure.persistence.repository.FleetAssignmentJpaRepository;

import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.dispatchai.fleet.domain.model.FleetAssignment;

/** Establishes a durable row to lock, including for terminal-before-assignment. */
@Component
public class FleetAssignmentStore {
  private final FleetAssignmentJpaRepository repository;
  private final TransactionTemplate initialization;

  public FleetAssignmentStore(FleetAssignmentJpaRepository repository, PlatformTransactionManager manager) {
    this.repository = repository;
    initialization = new TransactionTemplate(manager);
    initialization.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public FleetAssignment lock(UUID deliveryId) {
    Objects.requireNonNull(deliveryId, "Delivery ID is required");
    // Initialize independently: a simultaneous first event may win the insert.
    // Catch outside that transaction so a unique-key loser does not poison the
    // caller's transaction. Every state change then shares the same row lock.
    try {
      initialization.executeWithoutResult(tx -> {
        if (!repository.existsById(deliveryId)) {
          repository.saveAndFlush(new FleetAssignment(deliveryId));
        }
      });
    } catch (DataIntegrityViolationException concurrentInsert) {
      if (!repository.existsById(deliveryId)) { throw concurrentInsert; }
    }
    return repository.findByDeliveryIdForUpdate(deliveryId)
        .orElseThrow(() -> new IllegalStateException("Lifecycle row is missing"));
  }
}
