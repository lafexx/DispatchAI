package com.dispatchai.agent.domain.exceptions;

public class InvalidDispatchRecommendationException extends RuntimeException {
  public InvalidDispatchRecommendationException(String message) {
    super(message);
  }
}
