package com.dispatchai.fleet.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "messaging_inbox", indexes = @Index(name = "ix_inbox_processed", columnList = "processed_at"))
public class InboxEntry {
  @Id private UUID id;
  @Column(nullable = false, length = 128) private String consumerName;
  @Column(nullable = false) private UUID eventId;
  @Column(name = "processed_at") private Instant processedAt;
  protected InboxEntry() {}
  public InboxEntry(UUID id, String consumerName, UUID eventId) {
    this.id = id; this.consumerName = consumerName; this.eventId = eventId;
  }
  public Instant getProcessedAt() { return processedAt; }
  public void processed() { processedAt = Instant.now(); }
}
