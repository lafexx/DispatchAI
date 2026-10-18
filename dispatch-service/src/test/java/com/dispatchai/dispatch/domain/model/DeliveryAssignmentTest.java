package com.dispatchai.dispatch.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.dispatchai.dispatch.domain.exception.InvalidAssignmentStateException;

class DeliveryAssignmentTest {

  private DeliveryAssignment createAssignment() {
    return new DeliveryAssignment(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  @Test
  void newAssignmentStartsActive() {
    var assignment = createAssignment();

    assertEquals(
        AssignmentStatus.ACTIVE,
        assignment.getStatus());
  }

  @Test
  void activeAssignmentCanBeCompleted() {
    var assignment = createAssignment();

    assignment.complete();

    assertEquals(
        AssignmentStatus.COMPLETED,
        assignment.getStatus());
  }

  @Test
  void activeAssignmentCanBeCancelled() {
    var assignment = createAssignment();

    assignment.cancel();

    assertEquals(
        AssignmentStatus.CANCELLED,
        assignment.getStatus());
  }

  @Test
  void completedAssignmentCannotBeCancelled() {
    var assignment = createAssignment();

    assignment.complete();

    assertThrows(
        InvalidAssignmentStateException.class,
        assignment::cancel);
  }

  @Test
  void cancelledAssignmentCannotBeCompleted() {
    var assignment = createAssignment();

    assignment.cancel();

    assertThrows(
        InvalidAssignmentStateException.class,
        assignment::complete);
  }
}
