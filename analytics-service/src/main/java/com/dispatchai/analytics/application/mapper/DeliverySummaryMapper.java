package com.dispatchai.analytics.application.mapper;
import com.dispatchai.analytics.domain.model.SummaryAggregate;
import com.dispatchai.analytics.presentation.dto.DeliverySummaryResponse;
public final class DeliverySummaryMapper {
  private DeliverySummaryMapper() {}
  public static DeliverySummaryResponse toResponse(SummaryAggregate state) {
    return new DeliverySummaryResponse(state.total(),state.pending(),state.assigned(),state.inTransit(),state.completed(),state.cancelled(),state.delayed(),
        state.averageDelayMinutes(),state.averageAssignmentTimeSeconds(),state.averageDeliveryTimeSeconds(),state.averageTransitTimeSeconds(),
        state.completionRate(),state.cancellationRate(),state.delayRate());
  }
}
