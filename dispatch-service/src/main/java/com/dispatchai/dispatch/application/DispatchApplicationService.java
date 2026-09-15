package com.dispatchai.dispatch.application;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.dispatchai.dispatch.domain.model.AssignmentStatus;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.domain.model.exceptions.AssignmentNotFoundException;
import com.dispatchai.dispatch.infrastructure.persistence.DeliveryAssignmentJpaRepository;
import com.dispatchai.dispatch.presentation.dto.AssignmentResponse;
import com.dispatchai.dispatch.presentation.dto.CreateAssignmentRequest;

@Service
public class DispatchApplicationService {

  private final DeliveryAssignmentJpaRepository repository;

  public DispatchApplicationService(
      DeliveryAssignmentJpaRepository repository) {
    this.repository = repository;
  }

  public AssignmentResponse create(
      CreateAssignmentRequest request) {

    if (repository.existsByDeliveryIdAndStatus(
        request.deliveryId(),
        AssignmentStatus.ACTIVE)) {

      throw new IllegalStateException(
          "Delivery already has an active assignment");
    }

    var assignment = new DeliveryAssignment(
        UUID.randomUUID(),
        request.deliveryId(),
        request.driverId(),
        request.vehicleId());

    repository.save(assignment);

    return toResponse(assignment);
  }

  public AssignmentResponse getByAssignmentId(UUID assignmentId) {
    var assignment = repository.findById(assignmentId)
        .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found: " + assignmentId));

    return toResponse(assignment);
  }

  public AssignmentResponse getByDeliveryId(UUID deliveryId) {
    var assignment = repository.findByDeliveryId(deliveryId)
        .orElseThrow(() -> new AssignmentNotFoundException("Assignment not found: " + deliveryId));

    return toResponse(assignment);
  }

  private AssignmentResponse toResponse(DeliveryAssignment assignment) {
    return new AssignmentResponse(assignment.getId(), assignment.getDeliveryId(), assignment.getDriverId(),
        assignment.getVehicleId(), assignment.getStatus());
  }
}
