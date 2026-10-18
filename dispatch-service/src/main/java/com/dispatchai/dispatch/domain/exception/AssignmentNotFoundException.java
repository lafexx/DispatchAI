package com.dispatchai.dispatch.domain.exception;

import java.util.UUID;

public class AssignmentNotFoundException
    extends RuntimeException {

  public AssignmentNotFoundException(UUID assignmentId) {
    super("Assignment not found: " + assignmentId);
  }
}
