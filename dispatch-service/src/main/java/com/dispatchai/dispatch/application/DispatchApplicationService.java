package com.dispatchai.dispatch.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.exception.DeliveryNotAssignableException;
import com.dispatchai.dispatch.domain.exception.AssignmentNotFoundException;
import com.dispatchai.dispatch.domain.exception.NoAvailableDriverException;
import com.dispatchai.dispatch.domain.exception.DeliveryNotFoundException;
import com.dispatchai.dispatch.domain.exception.ReservationConflictException;
import com.dispatchai.dispatch.infrastructure.client.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.client.delivery.dto.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.client.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.client.fleet.dto.ReservationRequest;
import com.dispatchai.dispatch.infrastructure.messaging.publisher.AssignmentEventPublisher;
import com.dispatchai.dispatch.infrastructure.messaging.event.DriverAssignedEvent;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.infrastructure.persistence.store.DeliveryAssignmentLifecycleStore;
import com.dispatchai.dispatch.presentation.dto.AssignmentResponse;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@Service
public class DispatchApplicationService {
  private final DeliveryAssignmentJpaRepository repository;
  private final FleetClient fleet;
  private final DeliveryClient deliveries;
  private final AssignmentEventPublisher publisher;
  private final DeliveryAssignmentLifecycleStore lifecycles;
  private final ReservationCleanupService cleanup;
  private final TransactionTemplate transactions;

  public DispatchApplicationService(DeliveryAssignmentJpaRepository repository, FleetClient fleet,
      DeliveryClient deliveries, AssignmentEventPublisher publisher, DeliveryAssignmentLifecycleStore lifecycles,
      ReservationCleanupService cleanup, PlatformTransactionManager manager) {
    this.repository = repository;
    this.fleet = fleet;
    this.deliveries = deliveries;
    this.publisher = publisher;
    this.lifecycles = lifecycles;
    this.cleanup = cleanup;
    transactions = new TransactionTemplate(manager);
  }

  public AssignmentResponse create(CreateAssignmentRequest request) {
    return assign(request.deliveryId(), request.driverId(), request.vehicleId(), false);
  }

  public AssignmentResponse autoAssign(UUID deliveryId, UUID vehicleId) {
    var existing = repository.findFirstByDeliveryIdAndStatusOrderByAssignedAtDesc(deliveryId, AssignmentStatus.ACTIVE);
    if (existing.isPresent()) {
      if (vehicleId != null && !Objects.equals(existing.get().getVehicleId(), vehicleId)) {
        throw new ReservationConflictException("Delivery already has a different active assignment");
      }
      return toResponse(existing.get());
    }
    var context = eligibility(deliveryId);
    if (context.availableDrivers().isEmpty()) throw new NoAvailableDriverException("No drivers are currently available");
    var vehicles = context.eligibleVehicles().stream().filter(v -> vehicleId == null || vehicleId.equals(v.id())).toList();
    if (vehicles.isEmpty()) throw new ReservationConflictException("No available vehicle can carry this parcel");
    ReservationConflictException last = null;
    for (var vehicle : vehicles) for (var driver : context.availableDrivers()) {
      try { return assign(deliveryId, driver.id(), vehicle.id(), true); }
      catch (ReservationConflictException raced) { last = raced; }
    }
    throw last != null ? last : new ReservationConflictException("No eligible pair could be reserved");
  }

  public com.dispatchai.dispatch.presentation.dto.DispatchEligibilityResponse eligibility(UUID deliveryId) {
    var delivery = transactions.execute(tx -> validatePending(deliveryId));
    DispatchEligibility.requireWeight(delivery.parcelWeightKg());
    var drivers = fleet.getAvailableDrivers();
    if (drivers == null) throw new IllegalStateException("Fleet returned no driver response");
    return new com.dispatchai.dispatch.presentation.dto.DispatchEligibilityResponse(deliveryId, delivery.parcelWeightKg(),
        drivers.stream().filter(d -> "AVAILABLE".equals(d.availability())).sorted(java.util.Comparator.comparing(d -> d.id().toString())).toList(),
        DispatchEligibility.vehicles(delivery.parcelWeightKg(), fleet.getVehicles()));
  }

  private AssignmentResponse assign(UUID deliveryId, UUID driverId, UUID vehicleId, boolean automatic) {
    var attemptId = UUID.randomUUID();
    var requestRef = new java.util.concurrent.atomic.AtomicReference<>(new ReservationRequest(deliveryId, driverId, vehicleId, null));
    DeliveryAssignment assignment;
    try {
      assignment = transactions.execute(tx -> {
        var lifecycle = lifecycles.lock(deliveryId);
        if (lifecycle.isTerminal()) {
          throw new DeliveryNotAssignableException("Delivery is terminal");
        }
        var active = repository.findFirstByDeliveryIdAndStatusOrderByAssignedAtDesc(deliveryId,
            AssignmentStatus.ACTIVE);
        if (active.isPresent()) {
          var previous = active.get();
          if ((!automatic && !previous.getDriverId().equals(driverId))
              || (vehicleId != null && !Objects.equals(previous.getVehicleId(), vehicleId))) {
            throw new ReservationConflictException("Delivery already has a different active assignment");
          }
          return previous; // Same request repeats return the same assignment/token.
        }
        var delivery = deliveries.getDelivery(deliveryId);
        requirePending(delivery);
        var eligible = DispatchEligibility.vehicles(delivery.parcelWeightKg(), fleet.getVehicles());
        var vehicle = eligible.stream().filter(v -> vehicleId == null || vehicleId.equals(v.id())).findFirst()
            .orElseThrow(() -> new ReservationConflictException("No available vehicle can carry this parcel"));
        var request = new ReservationRequest(deliveryId, driverId, vehicle.id(), delivery.parcelWeightKg());
        requestRef.set(request);
        var reservation = fleet.reserve(attemptId, request);
        if (reservation == null || !attemptId.equals(reservation.assignmentId())
            || !deliveryId.equals(reservation.deliveryId()) || !driverId.equals(reservation.driverId())
            || !Objects.equals(request.vehicleId(), reservation.vehicleId()) || !"RESERVED".equals(reservation.status())) {
          throw new ReservationConflictException("Fleet did not return the requested reservation");
        }
        // Cancellation may have won while the remote reservation was being acquired.
        var currentDelivery = deliveries.getDelivery(deliveryId);
        requirePending(currentDelivery);
        if (currentDelivery.parcelWeightKg() == null || currentDelivery.parcelWeightKg().compareTo(delivery.parcelWeightKg()) != 0)
          throw new ReservationConflictException("Parcel weight changed while assigning; retry");
        var prepared = repository.saveAndFlush(new DeliveryAssignment(attemptId, deliveryId, driverId, request.vehicleId()));
        publisher.publish(new DriverAssignedEvent(prepared.getAssignmentEventId(), prepared.getId(),
            deliveryId, driverId, delivery.customerId(), request.vehicleId(), prepared.getAssignedAt()));
        return prepared;
      });
    } catch (RuntimeException preparationFailed) {
      try {
        cleanup.compensate(attemptId, requestRef.get());
      } catch (RuntimeException compensationFailed) {
        preparationFailed.addSuppressed(compensationFailed);
      }
      throw preparationFailed;
    }
    Objects.requireNonNull(assignment, "Assignment transaction produced no result");
    // The assignment and immutable outbox command have committed together.
    // Kafka availability cannot turn this successful local commit into an HTTP error.
    return toResponse(assignment);
  }

  private DeliveryResponse validatePending(UUID deliveryId) {
    var lifecycle = lifecycles.lock(deliveryId);
    if (lifecycle.isTerminal()) {
      throw new DeliveryNotAssignableException("Delivery is terminal");
    }
    var delivery = deliveries.getDelivery(deliveryId);
    requirePending(delivery);
    return delivery;
  }

  private void requirePending(DeliveryResponse delivery) {
    if (delivery == null || delivery.status() != RemoteDeliveryStatus.PENDING) {
      throw new DeliveryNotAssignableException("Delivery must be pending before assignment");
    }
  }

  public void closeAssignment(UUID deliveryId, AssignmentStatus terminal, Instant occurredAt) {
    transactions.executeWithoutResult(tx -> {
      var lifecycle = lifecycles.lock(deliveryId);
      lifecycle.recordTerminal(terminal, occurredAt);
      for (var assignment : repository.findByDeliveryIdAndStatus(deliveryId, AssignmentStatus.ACTIVE)) {
        assignment.applyTerminalState(lifecycle.getTerminalStatus(), lifecycle.getTerminalAt());
      }
    });
  }

  public void rejectAssignment(UUID assignmentId, UUID deliveryId, String reason, Instant occurredAt) {
    rejectAssignment(assignmentId, deliveryId, reason, "PENDING", occurredAt);
  }

  public void rejectAssignment(UUID assignmentId, UUID deliveryId, String reason, String deliveryStatus,
      Instant occurredAt) {
    transactions.executeWithoutResult(tx -> {
      var lifecycle = lifecycles.lock(deliveryId);
      var assignment = repository.findById(assignmentId).orElse(null);
      if (assignment == null) return;
      if (!assignment.getDeliveryId().equals(deliveryId)) {
        throw new IllegalArgumentException("Rejected event has wrong delivery ID");
      }
      if ("CANCELLED".equals(deliveryStatus) || "COMPLETED".equals(deliveryStatus)) {
        var terminal = "CANCELLED".equals(deliveryStatus) ? AssignmentStatus.CANCELLED : AssignmentStatus.COMPLETED;
        lifecycle.recordTerminal(terminal, occurredAt);
        assignment.applyTerminalState(lifecycle.getTerminalStatus(), lifecycle.getTerminalAt());
      } else {
        assignment.reject(reason, occurredAt);
      }
      if (assignment.getStatus() != AssignmentStatus.ACTIVE) {
        cleanup.enqueue(assignment.getId(),
            new ReservationRequest(assignment.getDeliveryId(), assignment.getDriverId(), assignment.getVehicleId()));
      }
    });
  }

  public AssignmentResponse getByAssignmentId(UUID id) {
    return toResponse(repository.findById(id).orElseThrow(() -> new AssignmentNotFoundException(id)));
  }

  public AssignmentResponse getByDeliveryId(UUID id) {
    return toResponse(repository.findByDeliveryId(id).orElseThrow(() -> new DeliveryNotFoundException(id)));
  }

  public List<AssignmentResponse> getByDriverId(UUID id) {
    return repository.findByDriverId(id).stream().map(this::toResponse).toList();
  }

  private AssignmentResponse toResponse(DeliveryAssignment assignment) {
    return new AssignmentResponse(assignment.getId(), assignment.getDeliveryId(), assignment.getDriverId(),
        assignment.getVehicleId(), assignment.getStatus(), assignment.getAssignedAt(), assignment.getCompletedAt(),
        assignment.getCancelledAt());
  }
}
