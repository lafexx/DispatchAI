package com.dispatchai.dispatch.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import com.dispatchai.dispatch.domain.model.AssignmentStatus;

public record AssignmentResponse(
    UUID id,
    UUID deliveryId,
    UUID driverId,
    UUID vehicleId,
    AssignmentStatus status,
    Instant assignedAt,
    Instant completedAt,
    Instant cancelledAt) {
}
