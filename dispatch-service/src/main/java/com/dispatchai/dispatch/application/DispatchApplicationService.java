package com.dispatchai.dispatch.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.RemoteDeliveryStatus;
import com.dispatchai.dispatch.domain.model.exceptions.AssignmentNotFoundException;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotAssignableException;
import com.dispatchai.dispatch.domain.model.exceptions.DeliveryNotFoundException;
import com.dispatchai.dispatch.domain.model.exceptions.NoAvailableDriverException;
import com.dispatchai.dispatch.infrastructure.delivery.DeliveryClient;
import com.dispatchai.dispatch.infrastructure.delivery.DeliveryResponse;
import com.dispatchai.dispatch.infrastructure.fleet.FleetClient;
import com.dispatchai.dispatch.infrastructure.messaging.AssignmentEventPublisher;
import com.dispatchai.dispatch.infrastructure.messaging.DriverAssignedEvent;
import com.dispatchai.dispatch.infrastructure.persistence.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.presentation.dto.AssignmentResponse;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@Service
public class DispatchApplicationService {

  private final DeliveryAssignmentJpaRepository repository;
  private final FleetClient fleetClient;
  private final DeliveryClient deliveryClient;
  private final AssignmentEventPublisher eventPublisher;

  public DispatchApplicationService(
      DeliveryAssignmentJpaRepository repository,
      FleetClient fleetClient,
      DeliveryClient deliveryClient,
      AssignmentEventPublisher eventPublisher) {
    this.repository = repository;
    this.fleetClient = fleetClient;
    this.deliveryClient = deliveryClient;
    this.eventPublisher = eventPublisher;
  }

  public AssignmentResponse create(
      CreateAssignmentRequest request) {

    return assign(
        request.deliveryId(),
        request.driverId(),
        request.vehicleId());
  }

  public AssignmentResponse autoAssign(
      UUID deliveryId,
      UUID vehicleId) {

    var drivers = fleetClient.getAvailableDrivers();

    if (drivers == null || drivers.isEmpty()) {
      throw new NoAvailableDriverException(
          "No drivers are currently available");
    }

    return assign(
        deliveryId,
        drivers.getFirst().id(),
        vehicleId);
  }

  private AssignmentResponse assign(
      UUID deliveryId,
      UUID driverId,
      UUID vehicleId) {

    var delivery = validateDeliveryCanBeAssigned(deliveryId);

    ensureNoActiveAssignment(deliveryId);

    var driver = fleetClient.getDriver(driverId);

    if (!"AVAILABLE".equals(driver.availability())) {
      throw new IllegalStateException(
          "Driver is not available");
    }

    var assignment = new DeliveryAssignment(
        UUID.randomUUID(),
        deliveryId,
        driverId,
        vehicleId);

    repository.save(assignment);

    eventPublisher.publish(
        new DriverAssignedEvent(
            UUID.randomUUID(),
            assignment.getId(),
            assignment.getDeliveryId(),
            assignment.getDriverId(),
            delivery.customerId(),
            assignment.getVehicleId(),
            Instant.now()));

    return toResponse(assignment);
  }

  private DeliveryResponse validateDeliveryCanBeAssigned(UUID deliveryId) {
    var delivery = deliveryClient.getDelivery(deliveryId);

    if (delivery.status() != RemoteDeliveryStatus.PENDING)
      throw new DeliveryNotAssignableException(
          "Delivery must be pending before assignment");

    return delivery;
  }

  public AssignmentResponse getByAssignmentId(UUID assignmentId) {
    var assignment = repository.findById(assignmentId)
        .orElseThrow(() -> new AssignmentNotFoundException(assignmentId));

    return toResponse(assignment);
  }

  public AssignmentResponse getByDeliveryId(UUID deliveryId) {
    var assignment = repository.findByDeliveryId(deliveryId)
        .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));

    return toResponse(assignment);
  }

  public List<AssignmentResponse> getByDriverId(UUID driverId) {
    var assignments = repository.findByDriverId(driverId);

    return toResponse(assignments);
  }

  private void ensureNoActiveAssignment(UUID deliveryId) {
    if (repository.existsByDeliveryIdAndStatus(deliveryId, AssignmentStatus.ACTIVE)) {
      throw new IllegalStateException("Delivery already has an active assignment");
    }

    return;
  }

  private AssignmentResponse toResponse(DeliveryAssignment assignment) {
    return new AssignmentResponse(
        assignment.getId(),
        assignment.getDeliveryId(),
        assignment.getDriverId(),
        assignment.getVehicleId(),
        assignment.getStatus(),
        assignment.getAssignedAt(),
        assignment.getCompletedAt(),
        assignment.getCancelledAt());
  }

  private List<AssignmentResponse> toResponse(List<DeliveryAssignment> assignments) {
    List<AssignmentResponse> responses = new ArrayList<AssignmentResponse>();

    for (DeliveryAssignment assignment : assignments) {
      responses.add(toResponse(assignment));
    }

    return responses;
  }
}
