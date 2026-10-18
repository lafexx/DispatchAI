package com.dispatchai.dispatch.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryAssignmentTerminalEventTest {
  @Test
  void duplicateCompletionPreservesProducerTimestamp() {
    var assignment = new DeliveryAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
    var occurredAt = Instant.parse("2026-10-12T01:00:00Z");
    assertTrue(assignment.applyTerminalState(AssignmentStatus.COMPLETED, occurredAt));
    assertFalse(assignment.applyTerminalState(AssignmentStatus.COMPLETED, occurredAt.plusSeconds(60)));
    assertEquals(occurredAt, assignment.getCompletedAt());
    assertNull(assignment.getCancelledAt());
  }

  @Test
  void conflictingTerminalEventDoesNotReplaceFirstFact() {
    var assignment = new DeliveryAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null);
    var occurredAt = Instant.parse("2026-10-12T01:00:00Z");
    assertTrue(assignment.applyTerminalState(AssignmentStatus.CANCELLED, occurredAt));
    assertFalse(assignment.applyTerminalState(AssignmentStatus.COMPLETED, occurredAt.plusSeconds(1)));
    assertEquals(AssignmentStatus.CANCELLED, assignment.getStatus());
    assertEquals(occurredAt, assignment.getCancelledAt());
    assertNull(assignment.getCompletedAt());
  }
}
