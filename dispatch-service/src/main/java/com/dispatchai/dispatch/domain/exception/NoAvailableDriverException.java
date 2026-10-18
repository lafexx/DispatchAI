package com.dispatchai.dispatch.domain.exception;

public class NoAvailableDriverException extends RuntimeException {
  public NoAvailableDriverException(String message) {
    super(message);
  }
}
