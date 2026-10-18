package com.dispatchai.agent.infrastructure.ai.tools;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.dispatchai.agent.application.DispatchRecommendationContextService;
import com.dispatchai.agent.application.model.DispatchRecommendationContext;

import dev.langchain4j.agent.tool.Tool;

@Component
public class DispatchTools {

  private final DispatchRecommendationContextService contextService;

  public DispatchTools(
      DispatchRecommendationContextService contextService) {
    this.contextService = contextService;
  }

  @Tool("""
      Get the delivery and all currently available drivers,
      including performance analytics, parcel weight and eligible vehicles with capacity, for dispatch recommendation.
      """)
  public DispatchRecommendationContext getDispatchRecommendationContext(UUID deliveryId) {

    return contextService.get(deliveryId);
  }
}
