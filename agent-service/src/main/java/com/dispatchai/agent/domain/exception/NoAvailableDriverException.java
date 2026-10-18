package com.dispatchai.agent.domain.exception;

public class NoAvailableDriverException extends RuntimeException {
  public NoAvailableDriverException() {
    super("No drivers are currently available");
  }
}
