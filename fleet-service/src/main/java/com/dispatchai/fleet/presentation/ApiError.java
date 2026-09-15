package com.dispatchai.fleet.presentation;

import java.time.Instant;

import org.springframework.http.HttpStatus;

public record ApiError(
    int status,
    String error,
    String message,
    Instant timestamp) {

  public static ApiError of(
      HttpStatus status,
      String message) {

    return new ApiError(
        status.value(),
        status.getReasonPhrase(),
        message,
        Instant.now());
  }
}
