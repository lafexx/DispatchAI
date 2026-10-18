package com.dispatchai.dispatch.application;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
import com.dispatchai.dispatch.domain.exception.ReservationConflictException;
/** Java owns resource eligibility; AI may only explain choices inside this set. */
public final class DispatchEligibility {
  private DispatchEligibility() {}
  public static void requireWeight(BigDecimal weight) {
    if (weight == null || weight.signum() <= 0) throw new ReservationConflictException("Delivery has no valid parcel weight; set its parcel weight before dispatch");
  }
  public static List<VehicleResponse> vehicles(BigDecimal weight, List<VehicleResponse> vehicles) {
    requireWeight(weight);
    if (vehicles == null) throw new IllegalStateException("Fleet returned no vehicle response");
    return vehicles.stream().filter(v -> v.available() && v.capacityKg() != null && v.capacityKg().compareTo(weight) >= 0)
        .sorted(Comparator.comparing(VehicleResponse::capacityKg).thenComparing(v -> v.id().toString())).toList();
  }
}
