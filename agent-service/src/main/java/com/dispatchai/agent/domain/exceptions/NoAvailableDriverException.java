package com.dispatchai.agent.domain.exceptions;

public class NoAvailableDriverException extends RuntimeException {
  public NoAvailableDriverException() {
    super("No drivers are currently available");
  }
}
