package com.dispatchai.agent.application;

import com.dispatchai.agent.infrastructure.ai.agent.DispatchAdvisorAgent;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.agent.domain.exception.InvalidDispatchRecommendationException;
import com.dispatchai.agent.domain.exception.NoAvailableDriverException;
import com.dispatchai.agent.application.model.DispatchRecommendationContext;
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

    if (context.eligibleVehicles().isEmpty())
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "No available vehicle can carry this parcel");
    var contextJson = serialize(context);

    com.dispatchai.agent.application.model.DispatchAdviceDraft advice;
    try { advice = agent.advise(contextJson); }
    catch (RuntimeException error) {
      throw new org.springframework.web.server.ResponseStatusException(timeout(error) ? org.springframework.http.HttpStatus.GATEWAY_TIMEOUT : org.springframework.http.HttpStatus.BAD_GATEWAY,
          "Advisor request failed or returned an unreadable recommendation", error);
    }
    if (advice == null || advice.reasoning() == null || advice.reasoning().isBlank() || advice.reasoning().length() > 4000)
      throw new InvalidDispatchRecommendationException("Advisor must return a nonempty explanation of at most 4000 characters");

    UUID recommendedDriverId;
    UUID recommendedVehicleId;

    try {
      recommendedDriverId = UUID.fromString(advice.recommendedDriverId());
      recommendedVehicleId = UUID.fromString(advice.recommendedVehicleId());
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw new InvalidDispatchRecommendationException(
          "Advisor returned an invalid driver or vehicle ID");
    }

    var candidate = context.availableDrivers()
        .stream()
        .filter(it -> it.driver().id().equals(recommendedDriverId))
        .findFirst()
        .orElseThrow(() -> new InvalidDispatchRecommendationException(
            "Advisor recommended a driver that was not in the available candidate set"));

    var vehicle = context.eligibleVehicles().stream().filter(v -> v.id().equals(recommendedVehicleId)).findFirst()
        .orElseThrow(() -> new InvalidDispatchRecommendationException("Advisor recommended a vehicle outside the eligible set"));
    // Refresh after potentially slow inference. Dispatch/Fleet recheck again at assignment time.
    var current = contextService.get(deliveryId);
    if (current.delivery().parcelWeightKg().compareTo(context.delivery().parcelWeightKg()) != 0
        || current.availableDrivers().stream().noneMatch(c -> c.driver().id().equals(recommendedDriverId))
        || current.eligibleVehicles().stream().noneMatch(v -> v.id().equals(recommendedVehicleId)))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
          "Recommended resources are no longer available; request new advice");
    // Do not trust the model's name either.
    var actualName = candidate.driver().name();

    return new DispatchAdviceResponse(
        recommendedDriverId,
        actualName,
        advice.reasoning(), recommendedVehicleId, vehicle.registration(), context.delivery().parcelWeightKg(),
        vehicle.capacityKg(), context.availableDrivers(), context.eligibleVehicles());
  }

  private boolean timeout(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof java.net.http.HttpTimeoutException || cause instanceof java.net.SocketTimeoutException
          || cause instanceof java.util.concurrent.TimeoutException) return true;
    }
    return false;
  }

  private String serialize(
      DispatchRecommendationContext context) {

    return objectMapper.writeValueAsString(context);
  }
}
