package com.dispatchai.dispatch.infrastructure.messaging.outbox;

import com.dispatchai.dispatch.infrastructure.persistence.repository.OutboxJpaRepository;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name = "messaging.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {
  private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);
  private final OutboxJpaRepository repository;
  private final OutboxTransport transport;
  private final TransactionTemplate transactions;
  public OutboxRelay(OutboxJpaRepository repository, OutboxTransport transport, PlatformTransactionManager manager) {
    this.repository = repository; this.transport = transport;
    transactions = new TransactionTemplate(manager);
    transactions.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }
  @Scheduled(fixedDelayString = "${messaging.outbox.poll-ms:1000}", initialDelayString = "${messaging.outbox.poll-ms:1000}")
  public void relay() {
    for (var id : repository.ready(Instant.now(), PageRequest.of(0, 50))) {
      try { publishOne(id); }
      catch (RuntimeException failure) {
        // Database/commit failures leave the original row retryable, even after a broker ACK.
        log.warn("Outbox transaction remains pending id={}", id, failure);
      }
    }
  }
  public void publishOne(Long id) {
    transactions.executeWithoutResult(tx -> {
      var entry = repository.lock(id).orElseThrow();
      if (!entry.due(Instant.now())
          || repository.existsByAggregateIdAndIdLessThanAndPublishedAtIsNull(entry.getAggregateId(), entry.getId())) return;
      try {
        // A bounded broker wait holds THIS row only, not a business aggregate lock.
        transport.send(entry);
        entry.published(Instant.now());
      } catch (RuntimeException failure) {
        entry.failed(Instant.now(), failure);
        log.warn("Outbox retry event={} correlation={} attempts={}", entry.getEventId(),
            entry.getCorrelationId(), entry.getAttempts(), failure);
      }
    });
  }
}
