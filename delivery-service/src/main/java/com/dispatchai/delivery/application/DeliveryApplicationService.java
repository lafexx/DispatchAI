package com.dispatchai.delivery.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dispatchai.delivery.domain.model.Delivery;
import com.dispatchai.delivery.domain.exception.DeliveryNotFoundException;
import com.dispatchai.delivery.infrastructure.messaging.publisher.DeliveryEventPublisher;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCancelledEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCompletedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryCreatedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryDelayedEvent;
import com.dispatchai.delivery.infrastructure.messaging.event.DeliveryStartedEvent;
import com.dispatchai.delivery.infrastructure.persistence.repository.DeliveryJpaRepository;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.DeliveryResponse;

@Service
public class DeliveryApplicationService {
  private final DeliveryJpaRepository repository;
  private final DeliveryEventPublisher eventPublisher;
  private final com.dispatchai.delivery.infrastructure.client.fleet.FleetReservationClient reservations;

  public DeliveryApplicationService(
      DeliveryJpaRepository repository,
      DeliveryEventPublisher eventPublisher,
      com.dispatchai.delivery.infrastructure.client.fleet.FleetReservationClient reservations) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.reservations = reservations;
  }

  @Transactional
  public DeliveryResponse create(CreateDeliveryRequest request) {
    var delivery = new Delivery(UUID.randomUUID(), request.customerId(), request.pickupAddress(),
        request.destinationAddress(), request.parcelWeightKg());

    repository.save(delivery);

    eventPublisher.publish(
        new DeliveryCreatedEvent(
            UUID.randomUUID(),
            delivery.getId(),
            delivery.getCustomerId(),
            delivery.getPickupAddress(),
            delivery.getDestinationAddress(),
            Instant.now()));

    return toResponse(delivery);
  }

  @Transactional
  public DeliveryResponse setParcelWeight(UUID id, java.math.BigDecimal weight) {
    var delivery = repository.findByIdForUpdate(id).orElseThrow(() -> new DeliveryNotFoundException(id));
    delivery.setParcelWeightKg(weight);
    repository.save(delivery);
    return toResponse(delivery);
  }

  public DeliveryResponse get(UUID id) {
    var delivery = repository.findById(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    return toResponse(delivery);
  }

  @Transactional
  public DeliveryResponse assign(UUID id, UUID driverId) {
    var delivery = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    delivery.assign(driverId);

    repository.save(delivery);

    return toResponse(delivery);
  }

  @Transactional
  public void assignFromEvent(UUID id, UUID driverId) {
    var delivery = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));
    if (delivery.applyAssignment(driverId)) { repository.save(delivery); }
  }

  @Transactional
  public void acceptAssignment(com.dispatchai.delivery.infrastructure.messaging.event.DriverAssignedEvent event) {
    var delivery = repository.findByIdForUpdate(event.deliveryId())
        .orElseThrow(() -> new DeliveryNotFoundException(event.deliveryId()));
    if (delivery.hasAssignment(event.assignmentId(), event.driverId(), event.vehicleId())) {
      publishAccepted(delivery, event); // A stable outcome ID reuses the original outbox snapshot.
      return;
    }
    if (delivery.getStatus() != com.dispatchai.delivery.domain.model.DeliveryStatus.PENDING) {
      publishRejected(delivery, event, "Delivery already " + delivery.getStatus());
      return;
    }
    try {
      // Confirmation and compensation serialize in Fleet. A stale GET check
      // would permit acceptance after a reservation was concurrently released.
      reservations.confirm(event.assignmentId(), event.deliveryId(), event.driverId(), event.vehicleId(), delivery.getParcelWeightKg());
    } catch (com.dispatchai.delivery.infrastructure.client.fleet.exception.ReservationDeclinedException declined) {
      publishRejected(delivery, event, declined.getMessage());
      return;
    }
    // Transport/5xx/ambiguous errors propagate for Kafka retry, not rejection.
    delivery.acceptReservedAssignment(event.assignmentId(), event.driverId(), event.vehicleId(),
        Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
    repository.save(delivery);
    publishAccepted(delivery, event);
  }

  private void publishAccepted(Delivery delivery,
      com.dispatchai.delivery.infrastructure.messaging.event.DriverAssignedEvent event) {
    eventPublisher.publish(new com.dispatchai.delivery.infrastructure.messaging.event.DeliveryAssignedEvent(
        outcomeEventId(event.assignmentId(), "accepted"), event.assignmentId(), event.deliveryId(),
        event.driverId(), delivery.getCustomerId(), event.vehicleId(), delivery.getAssignmentAcceptedAt()));
  }

  private void publishRejected(Delivery delivery,
      com.dispatchai.delivery.infrastructure.messaging.event.DriverAssignedEvent event, String reason) {
    boolean terminal = delivery.getStatus() == com.dispatchai.delivery.domain.model.DeliveryStatus.COMPLETED
        || delivery.getStatus() == com.dispatchai.delivery.domain.model.DeliveryStatus.CANCELLED;
    eventPublisher.publish(new com.dispatchai.delivery.infrastructure.messaging.event.AssignmentRejectedEvent(
        outcomeEventId(event.assignmentId(), "rejected"), event.assignmentId(), event.deliveryId(),
        event.driverId(), event.vehicleId(), reason, delivery.getStatus().name(), terminal ? delivery.getUpdatedAt() : Instant.now()));
  }

  private UUID outcomeEventId(UUID assignmentId, String outcome) {
    return UUID.nameUUIDFromBytes((assignmentId + ":" + outcome).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  @Transactional
  public DeliveryResponse start(UUID id) {
    var delivery = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    delivery.start();
    repository.save(delivery);

    eventPublisher.publish(new DeliveryStartedEvent(
        UUID.randomUUID(),
        delivery.getId(),
        delivery.getCustomerId(),
        delivery.getAssignedDriverId(),
        Instant.now()));

    return toResponse(delivery);
  }

  @Transactional
  public DeliveryResponse complete(UUID id) {
    var delivery = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    delivery.complete();
    repository.save(delivery);

    eventPublisher.publish(new DeliveryCompletedEvent(
        UUID.randomUUID(),
        delivery.getId(),
        delivery.getCustomerId(),
        delivery.getAssignedDriverId(),
        Instant.now()));

    return toResponse(delivery);
  }

  @Transactional
  public DeliveryResponse cancel(UUID id, CancelDeliveryRequest request) {
    var delivery = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    delivery.cancel(request.reason(), request.notes());
    repository.save(delivery);

    eventPublisher.publish(new DeliveryCancelledEvent(
        UUID.randomUUID(),
        delivery.getId(),
        delivery.getCustomerId(),
        delivery.getAssignedDriverId(),
        request.reason(),
        request.notes(),
        Instant.now()));

    return toResponse(delivery);
  }

  @Transactional
  public DeliveryResponse delay(
      UUID id,
      int delayMinutes,
      String reason) {

    var delivery = repository.findByIdForUpdate(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    delivery.markDelayed(delayMinutes, reason);

    repository.save(delivery);

    eventPublisher.publish(
        new DeliveryDelayedEvent(
            UUID.randomUUID(),
            delivery.getId(),
            delivery.getCustomerId(),
            delivery.getAssignedDriverId(),
            delayMinutes,
            reason,
            Instant.now()));

    return toResponse(delivery);
  }

  private DeliveryResponse toResponse(Delivery delivery) {
    return new DeliveryResponse(
        delivery.getId(),
        delivery.getCustomerId(),
        delivery.getPickupAddress(),
        delivery.getDestinationAddress(),
        delivery.getStatus(),
        delivery.getAssignedDriverId(),
        delivery.isDelayed(),
        delivery.getDelayMinutes(),
        delivery.getDelayReason(),
        delivery.getParcelWeightKg());
  }
}

