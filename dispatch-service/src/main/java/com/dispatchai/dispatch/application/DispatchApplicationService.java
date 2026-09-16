package com.dispatchai.dispatch.application;

import java.time.Instant;
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
    validateDeliveryCanBeAssigned(request.deliveryId());

    if (repository.existsByDeliveryIdAndStatus(
        request.deliveryId(),
        AssignmentStatus.ACTIVE)) {

      throw new IllegalStateException(
          "Delivery already has an active assignment");
    }

    var driver = fleetClient.getDriver(request.driverId());

    if (!"AVAILABLE".equals(driver.availability())) {
      throw new IllegalStateException("Driver is not available");
    }

    var assignment = new DeliveryAssignment(
        UUID.randomUUID(),
        request.deliveryId(),
        request.driverId(),
        request.vehicleId());

    repository.save(assignment);

    eventPublisher.publish(new DriverAssignedEvent(UUID.randomUUID(), assignment.getId(), assignment.getDeliveryId(),
        assignment.getDriverId(), assignment.getVehicleId(), Instant.now()));

    return toResponse(assignment);
  }

  private void validateDeliveryCanBeAssigned(UUID deliveryId) {
    var delivery = deliveryClient.getDelivery(deliveryId);

    if (delivery.status() != RemoteDeliveryStatus.PENDING)
      throw new DeliveryNotAssignableException(
          "Delivery must be pending before assignment");

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

  public AssignmentResponse autoAssign(UUID deliveryId, UUID vehicleId) {
    validateDeliveryCanBeAssigned(deliveryId);
    ensureNoActiveAssignment(deliveryId);

    var availableDrivers = fleetClient.getAvailableDrivers();

    if (availableDrivers == null || availableDrivers.isEmpty()) {
      throw new NoAvailableDriverException("No drivers are currently available");
    }

    var driver = availableDrivers.getFirst();

    var assignment = new DeliveryAssignment(UUID.randomUUID(), deliveryId, driver.id(),
        vehicleId);

    repository.save(assignment);

    eventPublisher.publish(
        new DriverAssignedEvent(
            UUID.randomUUID(),
            assignment.getId(),
            assignment.getDeliveryId(),
            assignment.getDriverId(),
            assignment.getVehicleId(),
            Instant.now()));

    return toResponse(assignment);
  }

  private void ensureNoActiveAssignment(UUID deliveryId) {
    if (repository.existsByDeliveryIdAndStatus(deliveryId, AssignmentStatus.ACTIVE)) {
      throw new IllegalStateException("Delivery already has an active assignment");
    }

    return;
  }

  private AssignmentResponse toResponse(DeliveryAssignment assignment) {
    return new AssignmentResponse(assignment.getId(), assignment.getDeliveryId(), assignment.getDriverId(),
        assignment.getVehicleId(), assignment.getStatus());
  }
}
