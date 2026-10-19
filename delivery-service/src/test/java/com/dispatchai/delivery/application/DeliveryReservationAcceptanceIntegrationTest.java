package com.dispatchai.delivery.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dispatchai.delivery.domain.model.DeliveryStatus;
import com.dispatchai.delivery.domain.model.CancellationReason;
import com.dispatchai.delivery.infrastructure.client.fleet.exception.ReservationDeclinedException;
import com.dispatchai.delivery.infrastructure.client.fleet.FleetReservationClient;
import com.dispatchai.delivery.infrastructure.messaging.publisher.DeliveryEventPublisher;
import com.dispatchai.delivery.infrastructure.messaging.event.DriverAssignedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryAssignedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.AssignmentRejectedEvent;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;

@DataJpaTest(showSql = false)
@Import(DeliveryApplicationService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeliveryReservationAcceptanceIntegrationTest {
  @Autowired
  DeliveryApplicationService service;
  @MockitoBean
  DeliveryEventPublisher publisher;
  @MockitoBean
  FleetReservationClient fleet;

  private DriverAssignedEvent event() {
    var delivery = service.create(new CreateDeliveryRequest(UUID.randomUUID(), "Pickup", "Destination"));
    return new DriverAssignedEvent(UUID.randomUUID(), UUID.randomUUID(), delivery.id(), UUID.randomUUID(),
        delivery.customerId(), null, Instant.now());
  }

  @Test
  void validReservationIsConfirmedBeforeAcceptanceAndDuplicateHasStableOutcome() {
    var event = event();
    service.acceptAssignment(event);
    service.acceptAssignment(event);
    verify(fleet, times(1)).confirm(
        eq(event.assignmentId()),
        eq(event.deliveryId()),
        eq(event.driverId()),
        eq(event.vehicleId()),
        argThat(weight -> weight != null
            && weight.compareTo(java.math.BigDecimal.ONE) == 0));
    assertEquals(DeliveryStatus.ASSIGNED, service.get(event.deliveryId()).status());
    var outcomes = ArgumentCaptor.forClass(DeliveryAssignedEvent.class);
    verify(publisher, times(2)).publish(outcomes.capture());
    assertEquals(outcomes.getAllValues().get(0).eventId(), outcomes.getAllValues().get(1).eventId());
    assertEquals(outcomes.getAllValues().get(0).occurredAt().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
        outcomes.getAllValues().get(1).occurredAt());
  }

  @Test
  void cancellationWinningBeforeApplicationProducesRejectionAndNoConfirmation() {
    var event = event();
    service.cancel(event.deliveryId(), new CancelDeliveryRequest(CancellationReason.CUSTOMER_REQUEST, "Changed plans"));
    service.acceptAssignment(event);
    verifyNoInteractions(fleet);
    assertEquals(DeliveryStatus.CANCELLED, service.get(event.deliveryId()).status());
    verify(publisher).publish(any(AssignmentRejectedEvent.class));
    verify(publisher, never()).publish(any(DeliveryAssignedEvent.class));
  }

  @Test
  void releasedOrExpiredReservationLeavesDeliveryPending() {
    var event = event();
    doThrow(new ReservationDeclinedException("Expired")).when(fleet).confirm(any(), any(), any(), any(), any());
    service.acceptAssignment(event);
    assertEquals(DeliveryStatus.PENDING, service.get(event.deliveryId()).status());
    assertNull(service.get(event.deliveryId()).assignedDriverId());
    verify(publisher).publish(any(AssignmentRejectedEvent.class));
  }

  @Test
  void unknownConfirmationOutcomeRollsBackForRetryInsteadOfRejecting() {
    var event = event();
    doThrow(new IllegalStateException("Timed out after confirmation may have succeeded"))
        .when(fleet).confirm(any(), any(), any(), any(), any());
    assertThrows(IllegalStateException.class, () -> service.acceptAssignment(event));
    assertEquals(DeliveryStatus.PENDING, service.get(event.deliveryId()).status());
    verify(publisher, never()).publish(any(AssignmentRejectedEvent.class));
  }

  @Test
  void duplicateAfterCompletionDoesNotReconfirmOrRegressDelivery() {
    var event = event();
    service.acceptAssignment(event);
    service.start(event.deliveryId());
    service.complete(event.deliveryId());
    service.acceptAssignment(event);
    verify(fleet, times(1)).confirm(any(), any(), any(), any(), any());
    assertEquals(DeliveryStatus.COMPLETED, service.get(event.deliveryId()).status());
  }
}
