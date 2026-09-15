package com.dispatchai.dispatch.domain.model.exceptions;

public class NoAvailableDriverException extends RuntimeException {
  public NoAvailableDriverException(String message) {
    super(message);
  }
}
