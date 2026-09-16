package com.dispatchai.delivery.application;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.delivery.domain.model.Delivery;
import com.dispatchai.delivery.domain.model.exceptions.DeliveryNotFoundException;
import com.dispatchai.delivery.infrastructure.messaging.DeliveryEventPublisher;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryCancelledEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryCompletedEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryCreatedEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryDelayedEvent;
import com.dispatchai.delivery.infrastructure.messaging.events.DeliveryStartedEvent;
import com.dispatchai.delivery.infrastructure.persistence.DeliveryJpaRepository;
import com.dispatchai.delivery.presentation.dto.CancelDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.CreateDeliveryRequest;
import com.dispatchai.delivery.presentation.dto.DeliveryResponse;

@Service
public class DeliveryApplicationService {
  private final DeliveryJpaRepository repository;
  private final DeliveryEventPublisher eventPublisher;

  public DeliveryApplicationService(
      DeliveryJpaRepository repository,
      DeliveryEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  public DeliveryResponse create(CreateDeliveryRequest request) {
    var delivery = new Delivery(UUID.randomUUID(), request.customerId(), request.pickupAddress(),
        request.destinationAddress());

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

  public DeliveryResponse get(UUID id) {
    var delivery = repository.findById(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    return toResponse(delivery);
  }

  public DeliveryResponse assign(UUID id, UUID driverId) {
    var delivery = repository.findById(id)
        .orElseThrow(() -> new DeliveryNotFoundException(id));

    delivery.assign(driverId);

    repository.save(delivery);

    return toResponse(delivery);
  }

  public DeliveryResponse start(UUID id) {
    var delivery = repository.findById(id)
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

  public DeliveryResponse complete(UUID id) {
    var delivery = repository.findById(id)
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

  public DeliveryResponse cancel(UUID id, CancelDeliveryRequest request) {
    var delivery = repository.findById(id)
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

  public DeliveryResponse delay(
      UUID id,
      int delayMinutes,
      String reason) {

    var delivery = repository.findById(id)
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
        delivery.getDelayReason());
  }
}
