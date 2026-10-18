package com.dispatchai.delivery.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import com.dispatchai.delivery.domain.model.DeliveryStatus;
import com.dispatchai.delivery.domain.model.CancellationReason;
import com.dispatchai.delivery.infrastructure.client.fleet.FleetReservationClient;
import com.dispatchai.delivery.infrastructure.messaging.consumer.AssignmentEventConsumer;
import com.dispatchai.delivery.infrastructure.messaging.publisher.DeliveryEventPublisher;
import com.dispatchai.delivery.infrastructure.messaging.event.DriverAssignedEvent;
import com.dispatchai.delivery.infrastructure.messaging.outbox.OutboxWriter;
import com.dispatchai.delivery.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.delivery.infrastructure.persistence.repository.OutboxJpaRepository;
import com.dispatchai.delivery.infrastructure.persistence.repository.DeliveryJpaRepository;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({DeliveryApplicationService.class, DeliveryEventPublisher.class, OutboxWriter.class, InboxProcessor.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeliveryOutboxWorkflowIntegrationTest {
  @Autowired DeliveryApplicationService service;
  @Autowired DeliveryJpaRepository deliveries;
  @Autowired OutboxJpaRepository outbox;
  @Autowired InboxProcessor inbox;
  @MockitoBean FleetReservationClient fleet;
  @MockitoBean ObjectMapper mapper;
  @BeforeEach void defaults() { when(mapper.writeValueAsString(any())).thenReturn("{\"test\":true}"); }
  @Test void cancelledStateRollsBackIfTheOutboxWriteFails() {
    var delivery = service.create(new CreateDeliveryRequest(UUID.randomUUID(), "Pickup", "Destination"));
    when(mapper.writeValueAsString(any())).thenThrow(new IllegalStateException("Cannot serialize"));
    assertThrows(IllegalStateException.class, () -> service.cancel(delivery.id(),
        new CancelDeliveryRequest(CancellationReason.CUSTOMER_REQUEST, "Test")));
    assertEquals(DeliveryStatus.PENDING, service.get(delivery.id()).status());
    assertEquals(1, outbox.findAll().stream().filter(e -> e.getAggregateId().equals(delivery.id())).count());
  }
  @Test void confirmThenRollbackCanRetryAndEnqueueOneAcceptedOutcome() {
    var delivery = service.create(new CreateDeliveryRequest(UUID.randomUUID(), "Pickup", "Destination"));
    var event = new DriverAssignedEvent(UUID.randomUUID(), UUID.randomUUID(), delivery.id(), UUID.randomUUID(),
        delivery.customerId(), null, Instant.now());
    var consumer = new AssignmentEventConsumer().driverAssigned(service, inbox);
    var message = MessageBuilder.withPayload(event).setHeader("correlationId", "dispatch-flow").build();
    when(mapper.writeValueAsString(any())).thenThrow(new IllegalStateException("Outbox storage failed"));
    assertThrows(IllegalStateException.class, () -> consumer.accept(message));
    assertEquals(DeliveryStatus.PENDING, service.get(delivery.id()).status());
    doReturn("{\"accepted\":true}").when(mapper).writeValueAsString(any());
    consumer.accept(message); consumer.accept(message);
    assertEquals(DeliveryStatus.ASSIGNED, service.get(delivery.id()).status());
    // Remote confirmation already committed on the first attempt; Fleet's endpoint is idempotent.
    verify(fleet, times(2)).confirm(event.assignmentId(), event.deliveryId(), event.driverId(), event.vehicleId(), java.math.BigDecimal.ONE);
    var outcomes = outbox.findAll().stream().filter(e -> e.getAggregateId().equals(delivery.id())
        && "deliveryAssigned-out-0".equals(e.getBindingName())).toList();
    assertEquals(1, outcomes.size()); assertEquals(event.eventId().toString(), outcomes.get(0).getCausationId());
  }
}
