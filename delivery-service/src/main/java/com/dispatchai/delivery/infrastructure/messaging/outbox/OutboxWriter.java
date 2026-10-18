package com.dispatchai.delivery.infrastructure.messaging.outbox;

import com.dispatchai.delivery.infrastructure.persistence.entity.OutboxMessage;
import com.dispatchai.delivery.infrastructure.persistence.repository.OutboxJpaRepository;

import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;

@Component
public class OutboxWriter {
  private final OutboxJpaRepository repository;
  private final ObjectMapper mapper;
  private final String source;
  public OutboxWriter(OutboxJpaRepository repository, ObjectMapper mapper,
      @Value("${spring.application.name}") String source) {
    this.repository = repository; this.mapper = mapper; this.source = source;
  }
  @Transactional(propagation = Propagation.MANDATORY)
  public void enqueue(String binding, UUID eventId, UUID aggregateId, Object event) {
    Objects.requireNonNull(eventId); Objects.requireNonNull(aggregateId); Objects.requireNonNull(event);
    var existing = repository.findByEventId(eventId);
    if (existing.isPresent()) {
      if (!existing.get().getBindingName().equals(binding) || !existing.get().getAggregateId().equals(aggregateId)) {
        throw new IllegalArgumentException("Event ID reused for a different route or aggregate");
      }
      return; // Preserve the original immutable payload and metadata.
    }
    repository.save(new OutboxMessage(eventId, aggregateId, binding, mapper.writeValueAsString(event), source));
  }
}
