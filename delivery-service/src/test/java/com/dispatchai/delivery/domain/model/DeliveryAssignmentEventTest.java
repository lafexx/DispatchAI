package com.dispatchai.delivery.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import com.dispatchai.delivery.domain.exception.InvalidDeliveryStateException;

class DeliveryAssignmentEventTest {
  private Delivery pending() {
    return new Delivery(UUID.randomUUID(), UUID.randomUUID(), "Pickup", "Destination");
  }

  @Test
  void duplicateDoesNotRegressRunningDelivery() {
    var delivery = pending();
    var driver = UUID.randomUUID();
    assertTrue(delivery.applyAssignment(driver));
    delivery.start();
    assertFalse(delivery.applyAssignment(driver));
    assertEquals(DeliveryStatus.IN_TRANSIT, delivery.getStatus());
  }

  @Test
  void cancellationBeforeAssignmentStaysCancelledAndUnassigned() {
    var delivery = pending();
    delivery.cancel(CancellationReason.CUSTOMER_REQUEST, "Changed plans");
    assertFalse(delivery.applyAssignment(UUID.randomUUID()));
    assertEquals(DeliveryStatus.CANCELLED, delivery.getStatus());
    assertNull(delivery.getAssignedDriverId());
  }

  @Test
  void completedDeliveryIgnoresLateAssignment() {
    var delivery = pending();
    var driver = UUID.randomUUID();
    delivery.assign(driver);
    delivery.start();
    delivery.complete();
    assertFalse(delivery.applyAssignment(driver));
    assertEquals(DeliveryStatus.COMPLETED, delivery.getStatus());
  }

  @Test
  void conflictingActiveAssignmentIsRejected() {
    var delivery = pending();
    delivery.assign(UUID.randomUUID());
    assertThrows(InvalidDeliveryStateException.class,
        () -> delivery.applyAssignment(UUID.randomUUID()));
  }
}
