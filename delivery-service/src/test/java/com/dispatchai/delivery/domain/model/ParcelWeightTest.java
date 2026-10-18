package com.dispatchai.delivery.domain.model;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.dispatchai.delivery.domain.exception.InvalidDeliveryStateException;
class ParcelWeightTest {
  private Delivery delivery(String weight) { return new Delivery(UUID.randomUUID(),UUID.randomUUID(),"P","D",new BigDecimal(weight)); }
  @Test void positiveWeightIsStored() { assertEquals(new BigDecimal("12.50"),delivery("12.50").getParcelWeightKg()); }
  @Test void invalidWeightsAreRejected() {
    for (String weight : new String[]{"0","-1","0.001","100000000"}) assertThrows(IllegalArgumentException.class,()->delivery(weight));
  }
  @Test void pendingWeightCanChange() { var d=delivery("12"); d.setParcelWeightKg(new BigDecimal("50")); assertEquals(new BigDecimal("50"),d.getParcelWeightKg()); }
  @Test void assignedWeightCannotChange() { var d=delivery("12"); d.assign(UUID.randomUUID()); assertThrows(InvalidDeliveryStateException.class,()->d.setParcelWeightKg(new BigDecimal("50"))); }
}
