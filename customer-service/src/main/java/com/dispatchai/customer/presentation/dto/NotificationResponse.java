package com.dispatchai.customer.presentation.dto;

import java.time.Instant;
import java.util.UUID;

import com.dispatchai.customer.domain.model.NotificationStatus;
import com.dispatchai.customer.domain.model.NotificationType;

public record NotificationResponse(
    UUID id,
    UUID sourceEventId,
    UUID customerId,
    UUID deliveryId,
    NotificationType type,
    String message,
    NotificationStatus status,
    Instant createdAt) {
}
