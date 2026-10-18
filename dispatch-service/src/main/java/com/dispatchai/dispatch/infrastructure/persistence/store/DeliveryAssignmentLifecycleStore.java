package com.dispatchai.dispatch.infrastructure.persistence.store;

import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentLifecycleJpaRepository;

import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.dispatchai.dispatch.domain.model.DeliveryAssignmentLifecycle;

/** Establishes a durable row to lock, including for terminal-before-assignment. */
@Component
public class DeliveryAssignmentLifecycleStore {
  private final DeliveryAssignmentLifecycleJpaRepository repository;
  private final TransactionTemplate initialization;

  public DeliveryAssignmentLifecycleStore(DeliveryAssignmentLifecycleJpaRepository repository, PlatformTransactionManager manager) {
    this.repository = repository;
    initialization = new TransactionTemplate(manager);
    initialization.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public DeliveryAssignmentLifecycle lock(UUID deliveryId) {
    Objects.requireNonNull(deliveryId, "Delivery ID is required");
    // Initialize independently: a simultaneous first event may win the insert.
    // Catch outside that transaction so a unique-key loser does not poison the
    // caller's transaction. Every state change then shares the same row lock.
    try {
      initialization.executeWithoutResult(tx -> {
        if (!repository.existsById(deliveryId)) {
          repository.saveAndFlush(new DeliveryAssignmentLifecycle(deliveryId));
        }
      });
    } catch (DataIntegrityViolationException concurrentInsert) {
      if (!repository.existsById(deliveryId)) { throw concurrentInsert; }
    }
    return repository.findByDeliveryIdForUpdate(deliveryId)
        .orElseThrow(() -> new IllegalStateException("Lifecycle row is missing"));
  }
}
