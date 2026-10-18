package com.dispatchai.customer.infrastructure.messaging.inbox;

import com.dispatchai.customer.infrastructure.messaging.EventContext;
import com.dispatchai.customer.infrastructure.persistence.entity.InboxEntry;
import com.dispatchai.customer.infrastructure.persistence.repository.InboxJpaRepository;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class InboxProcessor {
  private final InboxJpaRepository repository;
  private final TransactionTemplate initialize;
  private final TransactionTemplate transactions;
  public InboxProcessor(InboxJpaRepository repository, PlatformTransactionManager manager) {
    this.repository = repository;
    initialize = new TransactionTemplate(manager);
    initialize.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    transactions = new TransactionTemplate(manager);
  }
  public void process(String consumer, UUID eventId, UUID aggregateId, MessageHeaders headers, Runnable handler) {
    Objects.requireNonNull(eventId, "Event ID is required");
    Objects.requireNonNull(aggregateId, "Aggregate ID is required");
    var previous = EventContext.enter(eventId, aggregateId, headers);
    try {
      var id = UUID.nameUUIDFromBytes((consumer + ":" + eventId).getBytes(StandardCharsets.UTF_8));
      // An unprocessed mutex may survive failure. It is NEVER proof of success.
      try {
        initialize.executeWithoutResult(tx -> {
          if (!repository.existsById(id)) repository.saveAndFlush(new InboxEntry(id, consumer, eventId));
        });
      } catch (DataIntegrityViolationException raced) {
        if (!repository.existsById(id)) throw raced;
      }
      transactions.executeWithoutResult(tx -> {
        var entry = repository.lock(id).orElseThrow();
        if (entry.getProcessedAt() != null) return;
        handler.run(); // Business writes, generated outbox rows and marker join THIS transaction.
        entry.processed();
      });
    } finally {
      EventContext.restore(previous);
    }
  }
}
