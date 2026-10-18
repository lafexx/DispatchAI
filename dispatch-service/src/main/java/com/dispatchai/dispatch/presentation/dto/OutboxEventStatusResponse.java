package com.dispatchai.dispatch.presentation.dto;
import java.util.UUID;
import java.time.Instant;
public record OutboxEventStatusResponse(UUID eventId, UUID aggregateId, String binding, Instant createdAt, Instant publishedAt, int attempts, Instant nextAttemptAt, String lastError) {}
