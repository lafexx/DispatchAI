package com.dispatchai.fleet.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FleetAssignmentTest {
  @Test
  void terminalBeforeAssignmentNeverAddsOrRemovesOccupancy() {
    var assignment = new FleetAssignment(UUID.randomUUID());
    var occurredAt = Instant.parse("2026-10-12T01:00:00Z");
    assertFalse(assignment.recordTerminal(FleetAssignmentStatus.CANCELLED, occurredAt));
    assertFalse(assignment.recordAssignment(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), occurredAt.minusSeconds(1)));
    assertEquals(FleetAssignmentStatus.CANCELLED, assignment.getStatus());
    assertEquals(occurredAt, assignment.getClosedAt());
  }

  @Test
  void eachTransitionChangesOccupancyOnlyOnce() {
    var assignment = new FleetAssignment(UUID.randomUUID());
    var id = UUID.randomUUID();
    var driver = UUID.randomUUID();
    var occurredAt = Instant.now();
    assertTrue(assignment.recordAssignment(id, driver, null, occurredAt));
    assertFalse(assignment.recordAssignment(id, driver, null, occurredAt));
    assertTrue(assignment.recordTerminal(FleetAssignmentStatus.COMPLETED, occurredAt));
    assertFalse(assignment.recordTerminal(FleetAssignmentStatus.COMPLETED, occurredAt.plusSeconds(1)));
    assertFalse(assignment.recordAssignment(id, driver, null, occurredAt));
  }
}
