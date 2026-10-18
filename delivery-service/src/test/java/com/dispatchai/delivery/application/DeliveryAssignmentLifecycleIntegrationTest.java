package com.dispatchai.delivery.application;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dispatchai.delivery.domain.model.CancellationReason;
import com.dispatchai.delivery.domain.model.DeliveryStatus;
import com.dispatchai.delivery.infrastructure.messaging.publisher.DeliveryEventPublisher;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;

@DataJpaTest(showSql = false)
@Import(DeliveryApplicationService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeliveryAssignmentLifecycleIntegrationTest {
  @Autowired
  DeliveryApplicationService service;
  @MockitoBean
  DeliveryEventPublisher publisher;
  @MockitoBean
  com.dispatchai.delivery.infrastructure.client.fleet.FleetReservationClient reservations;

  @Test
  void duplicateEventDoesNotRegressRunningOrCompletedDelivery() {
    var delivery = service.create(new CreateDeliveryRequest(UUID.randomUUID(), "Pickup", "Destination"));
    var driver = UUID.randomUUID();
    service.assignFromEvent(delivery.id(), driver);
    service.start(delivery.id());
    service.assignFromEvent(delivery.id(), driver);
    assertEquals(DeliveryStatus.IN_TRANSIT, service.get(delivery.id()).status());
    service.complete(delivery.id());
    service.assignFromEvent(delivery.id(), driver);
    assertEquals(DeliveryStatus.COMPLETED, service.get(delivery.id()).status());
  }

  @Test
  void lateAssignmentDoesNotResurrectCancelledPendingDelivery() {
    var delivery = service.create(new CreateDeliveryRequest(UUID.randomUUID(), "Pickup", "Destination"));
    service.cancel(delivery.id(), new CancelDeliveryRequest(CancellationReason.CUSTOMER_REQUEST, "Changed plans"));
    service.assignFromEvent(delivery.id(), UUID.randomUUID());
    assertEquals(DeliveryStatus.CANCELLED, service.get(delivery.id()).status());
    assertNull(service.get(delivery.id()).assignedDriverId());
  }
}
