package com.dispatchai.delivery.presentation.dto;
import java.util.UUID;
import java.time.Instant;
public record OutboxPendingResponse(UUID eventId, UUID aggregateId, String binding, String correlationId, Instant createdAt, int attempts, Instant nextAttemptAt, String lastError) {}
