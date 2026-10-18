package com.dispatchai.dispatch.infrastructure.persistence.entity;

import com.dispatchai.dispatch.infrastructure.messaging.EventContext;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "messaging_outbox", uniqueConstraints = @UniqueConstraint(name = "uk_outbox_event", columnNames = "event_id"),
    indexes = { @Index(name = "ix_outbox_due", columnList = "published_at,next_attempt_at"),
        @Index(name = "ix_outbox_aggregate", columnList = "aggregate_id,published_at,id") })
public class OutboxMessage {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "event_id", nullable = false) private UUID eventId;
  @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
  @Column(nullable = false, length = 128) private String bindingName;
  @Column(nullable = false, length = 128) private String eventType;
  @Column(nullable = false, length = 128) private String source;
  @Column(nullable = false) private int schemaVersion = 1;
  @Column(nullable = false, length = 512) private String correlationId;
  private String causationId;
  @Lob @Column(nullable = false) private String payload;
  @Column(nullable = false) private Instant createdAt;
  @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
  @Column(name = "published_at") private Instant publishedAt;
  @Column(nullable = false) private int attempts;
  @Column(length = 2000) private String lastError;
  protected OutboxMessage() {}
  public OutboxMessage(UUID eventId, UUID aggregateId, String binding, String payload, String source) {
    this.eventId = eventId; this.aggregateId = aggregateId; this.bindingName = binding;
    this.payload = payload; this.source = source; this.eventType = binding.replace("-out-0", "");
    var metadata = EventContext.current(aggregateId);
    correlationId = metadata.correlationId(); causationId = metadata.causationId();
    createdAt = Instant.now(); nextAttemptAt = createdAt;
  }
  public boolean due(Instant now) { return publishedAt == null && !nextAttemptAt.isAfter(now); }
  public void published(Instant now) { if (attempts < Integer.MAX_VALUE) attempts++; publishedAt = now; lastError = null; }
  public void failed(Instant now, RuntimeException failure) {
    if (attempts < Integer.MAX_VALUE) attempts++;
    nextAttemptAt = now.plusSeconds(Math.min(60L, 1L << Math.min(attempts - 1, 6)));
    var message = failure.getClass().getSimpleName() + ": " + failure.getMessage();
    lastError = message.substring(0, Math.min(2000, message.length()));
  }
  public Long getId() { return id; }
  public UUID getEventId() { return eventId; }
  public UUID getAggregateId() { return aggregateId; }
  public String getBindingName() { return bindingName; }
  public String getPayload() { return payload; }
  public String getSource() { return source; }
  public int getSchemaVersion() { return schemaVersion; }
  public String getEventType() { return eventType; }
  public String getCorrelationId() { return correlationId; }
  public String getCausationId() { return causationId; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getNextAttemptAt() { return nextAttemptAt; }
  public Instant getPublishedAt() { return publishedAt; }
  public int getAttempts() { return attempts; }
  public String getLastError() { return lastError; }
}
