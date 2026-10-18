package com.dispatchai.delivery.presentation;

import com.dispatchai.delivery.presentation.dto.OutboxEventStatusResponse;
import com.dispatchai.delivery.presentation.dto.OutboxPendingResponse;
import com.dispatchai.delivery.presentation.dto.OutboxStatusResponse;

import com.dispatchai.delivery.infrastructure.persistence.repository.OutboxJpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/messaging/outbox")
public class OutboxStatusController {
  private final OutboxJpaRepository repository;
  public OutboxStatusController(OutboxJpaRepository repository) { this.repository = repository; }
  @GetMapping
  @Transactional(readOnly = true)
  public OutboxStatusResponse status(@RequestParam(required = false) UUID aggregateId) {
    var pending = aggregateId == null ? repository.findTop50ByPublishedAtIsNullOrderByIdAsc()
        : repository.findTop50ByAggregateIdAndPublishedAtIsNullOrderByIdAsc(aggregateId);
    var entries = pending.stream()
        .map(e -> new OutboxPendingResponse(e.getEventId(), e.getAggregateId(), e.getBindingName(), e.getCorrelationId(),
            e.getCreatedAt(), e.getAttempts(), e.getNextAttemptAt(), e.getLastError())).toList();
    return new OutboxStatusResponse(repository.countByPublishedAtIsNull(), repository.countByPublishedAtIsNotNull(), entries);
  }
  @GetMapping("/{eventId}")
  @Transactional(readOnly = true)
  public org.springframework.http.ResponseEntity<OutboxEventStatusResponse> event(@PathVariable UUID eventId) {
    return repository.findByEventId(eventId).map(e -> org.springframework.http.ResponseEntity.ok(
        new OutboxEventStatusResponse(e.getEventId(), e.getAggregateId(), e.getBindingName(), e.getCreatedAt(),
            e.getPublishedAt(), e.getAttempts(), e.getNextAttemptAt(), e.getLastError())))
        .orElseGet(() -> org.springframework.http.ResponseEntity.notFound().build());
  }

}
