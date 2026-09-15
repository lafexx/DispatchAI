package com.dispatchai.dispatch.domain.model.exceptions;

public class AssignmentNotFoundException extends RuntimeException {
  public AssignmentNotFoundException(String message) {
    super(message);
  }
}
