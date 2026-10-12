package com.dispatchai.agent.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.agent.domain.exceptions.InvalidDispatchRecommendationException;
import com.dispatchai.agent.domain.exceptions.NoAvailableDriverException;
import com.dispatchai.agent.domain.model.DispatchRecommendationContext;
import com.dispatchai.agent.presentation.dto.DispatchAdviceResponse;

import tools.jackson.databind.ObjectMapper;

@Service
public class DispatchAdvisorService {

  private final DispatchRecommendationContextService contextService;
  private final DispatchAdvisorAgent agent;
  private final ObjectMapper objectMapper;

  public DispatchAdvisorService(
      DispatchRecommendationContextService contextService,
      DispatchAdvisorAgent agent,
      ObjectMapper objectMapper) {
    this.contextService = contextService;
    this.agent = agent;
    this.objectMapper = objectMapper;
  }

  public DispatchAdviceResponse advise(UUID deliveryId) {
    var context = contextService.get(deliveryId);

    if (context.availableDrivers().isEmpty()) {
      throw new NoAvailableDriverException();
    }

    var contextJson = serialize(context);

    var advice = agent.advise(contextJson);

    UUID recommendedDriverId;

    try {
      recommendedDriverId = UUID.fromString(advice.recommendedDriverId());
    } catch (IllegalArgumentException ex) {
      throw new InvalidDispatchRecommendationException(
          "Advisor returned an invalid driver ID");
    }

    var candidate = context.availableDrivers()
        .stream()
        .filter(it -> it.driver().id().equals(recommendedDriverId))
        .findFirst()
        .orElseThrow(() -> new InvalidDispatchRecommendationException(
            "Advisor recommended a driver that was not in the available candidate set"));

    // Do not trust the model's name either.
    var actualName = candidate.driver().name();

    return new DispatchAdviceResponse(
        recommendedDriverId,
        actualName,
        advice.reasoning());
  }

  private String serialize(
      DispatchRecommendationContext context) {

    return objectMapper.writeValueAsString(context);
  }
}
