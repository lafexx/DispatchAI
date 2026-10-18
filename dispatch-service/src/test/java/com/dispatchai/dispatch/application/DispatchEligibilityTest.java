package com.dispatchai.dispatch.application;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.VehicleResponse;
import com.dispatchai.dispatch.domain.exception.ReservationConflictException;
class DispatchEligibilityTest {
  private VehicleResponse vehicle(int id, String capacity, boolean available) {
    return new VehicleResponse(new UUID(0,id),"V"+id,"VAN",new BigDecimal(capacity),available);
  }
  @Test void excludesInsufficientCapacityAndUnavailableVehicles() {
    var exact=vehicle(1,"50",true);
    assertEquals(List.of(exact),DispatchEligibility.vehicles(new BigDecimal("50"),List.of(vehicle(2,"49.99",true),vehicle(3,"500",false),exact)));
  }
  @Test void selectsSmallestCapacityThenStableId() {
    var small=vehicle(1,"50",true); var tie=vehicle(2,"50",true); var big=vehicle(3,"500",true);
    assertEquals(List.of(small,tie,big),DispatchEligibility.vehicles(BigDecimal.ONE,List.of(big,tie,small)));
  }
  @Test void unknownAndNonpositiveWeightCannotBeDispatched() {
    assertThrows(ReservationConflictException.class,()->DispatchEligibility.vehicles(null,List.of()));
    assertThrows(ReservationConflictException.class,()->DispatchEligibility.vehicles(BigDecimal.ZERO,List.of()));
  }
}
