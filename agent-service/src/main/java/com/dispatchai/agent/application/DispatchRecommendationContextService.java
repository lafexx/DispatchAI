package com.dispatchai.agent.application;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.dispatchai.agent.application.model.DispatchCandidate;
import com.dispatchai.agent.application.model.DispatchRecommendationContext;
import com.dispatchai.agent.infrastructure.client.analytics.AnalyticsClient;
import com.dispatchai.agent.infrastructure.client.dispatch.DispatchClient;
import com.dispatchai.agent.infrastructure.client.delivery.DeliveryClient;
@Service
public class DispatchRecommendationContextService {
  private final DispatchClient dispatch;
  private final DeliveryClient deliveries;
  private final AnalyticsClient analytics;
  public DispatchRecommendationContextService(DispatchClient dispatch, DeliveryClient deliveries, AnalyticsClient analytics) {
    this.dispatch=dispatch; this.deliveries=deliveries; this.analytics=analytics;
  }
  public DispatchRecommendationContext get(UUID deliveryId) {
    var eligibility = dispatch.eligibility(deliveryId);
    var delivery = deliveries.getDelivery(deliveryId);
    if (eligibility == null || delivery == null || !deliveryId.equals(eligibility.deliveryId())
        || !"PENDING".equals(delivery.status()))
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "Delivery is no longer pending");
    if (delivery.parcelWeightKg() == null || eligibility.parcelWeightKg() == null || eligibility.parcelWeightKg().compareTo(delivery.parcelWeightKg()) != 0)
      throw new IllegalStateException("Dispatch eligibility does not match delivery weight");
    var candidates = eligibility.availableDrivers().stream()
        .map(d -> new DispatchCandidate(d, analytics.getDriver(d.id()))).toList();
    return new DispatchRecommendationContext(delivery, candidates, eligibility.eligibleVehicles());
  }
}
