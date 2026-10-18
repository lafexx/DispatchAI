package com.dispatchai.agent.domain.exception;

public class InvalidDispatchRecommendationException extends RuntimeException {
  public InvalidDispatchRecommendationException(String message) {
    super(message);
  }
}
