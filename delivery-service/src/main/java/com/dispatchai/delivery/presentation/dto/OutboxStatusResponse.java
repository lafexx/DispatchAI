package com.dispatchai.delivery.presentation.dto;
import java.util.UUID;
import java.time.Instant;
public record OutboxStatusResponse(long pending, long published, java.util.List<OutboxPendingResponse> oldestPending) {}
