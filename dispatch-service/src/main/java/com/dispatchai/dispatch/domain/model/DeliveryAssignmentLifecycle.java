package com.dispatchai.dispatch.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Retains terminal delivery knowledge even when no assignment exists yet. */
@Entity
@Table(name = "delivery_assignment_lifecycles")
public class DeliveryAssignmentLifecycle {
  @Id
  private UUID deliveryId;
  @Enumerated(EnumType.STRING)
  private AssignmentStatus terminalStatus;
  private Instant terminalAt;

  protected DeliveryAssignmentLifecycle() {}

  public DeliveryAssignmentLifecycle(UUID deliveryId) {
    this.deliveryId = Objects.requireNonNull(deliveryId, "Delivery ID is required");
  }

  public void recordTerminal(AssignmentStatus status, Instant occurredAt) {
    if (status != AssignmentStatus.COMPLETED && status != AssignmentStatus.CANCELLED) {
      throw new IllegalArgumentException("A terminal assignment status is required");
    }
    Objects.requireNonNull(occurredAt, "Event time is required");
    // A delivery cannot legitimately complete AND cancel. First terminal fact wins.
    if (!isTerminal()) {
      terminalStatus = status;
      terminalAt = occurredAt;
    }
  }

  public boolean isTerminal() { return terminalStatus != null; }
  public AssignmentStatus getTerminalStatus() { return terminalStatus; }
  public Instant getTerminalAt() { return terminalAt; }
}
