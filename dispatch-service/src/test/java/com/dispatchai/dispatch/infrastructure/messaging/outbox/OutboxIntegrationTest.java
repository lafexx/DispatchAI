package com.dispatchai.dispatch.infrastructure.messaging.outbox;

import com.dispatchai.dispatch.infrastructure.messaging.EventContext;
import com.dispatchai.dispatch.infrastructure.messaging.inbox.InboxProcessor;
import com.dispatchai.dispatch.infrastructure.persistence.entity.OutboxMessage;
import com.dispatchai.dispatch.infrastructure.persistence.repository.OutboxJpaRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;

@DataJpaTest(showSql = false, properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "messaging.outbox.poll-ms=3600000"})
@Import({OutboxWriter.class, OutboxRelay.class, InboxProcessor.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OutboxIntegrationTest {
  @Autowired OutboxWriter writer;
  @Autowired OutboxJpaRepository outbox;
  @Autowired OutboxRelay relay;
  @Autowired InboxProcessor inbox;
  @Autowired DeliveryAssignmentJpaRepository business;
  @Autowired PlatformTransactionManager manager;
  @MockitoBean OutboxTransport transport;
  @MockitoBean ObjectMapper mapper;
  private TransactionTemplate tx() { return new TransactionTemplate(manager); }
  @BeforeEach void defaults() {
    outbox.deleteAll(); when(mapper.writeValueAsString(any())).thenReturn("{\"eventId\":\"stable\"}");
  }
  private OutboxMessage enqueue(UUID aggregate, UUID eventId) {
    tx().executeWithoutResult(status -> writer.enqueue("test-out-0", eventId, aggregate, "payload"));
    return outbox.findByEventId(eventId).orElseThrow();
  }
  @Test void businessRecordAndEventCommitOrRollbackTogetherWithoutTouchingKafka() {
    var id = UUID.randomUUID(); var event = UUID.randomUUID();
    assertThrows(IllegalStateException.class, () -> tx().executeWithoutResult(status -> {
      business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null)); writer.enqueue("test-out-0", event, id, "payload");
      throw new IllegalStateException("Failed before commit");
    }));
    assertFalse(business.existsById(id)); assertTrue(outbox.findByEventId(event).isEmpty());
    tx().executeWithoutResult(status -> {
      business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null)); writer.enqueue("test-out-0", event, id, "payload");
    });
    assertTrue(business.existsById(id)); assertTrue(outbox.findByEventId(event).isPresent());
    verifyNoInteractions(transport);
  }
  @Test void queueRequiresTheBusinessTransactionAndStableIdsDoNotAppendAnotherRow() {
    var aggregate = UUID.randomUUID(); var event = UUID.randomUUID();
    assertThrows(IllegalTransactionStateException.class,
        () -> writer.enqueue("test-out-0", event, aggregate, "payload"));
    enqueue(aggregate, event); enqueue(aggregate, event); assertEquals(1, outbox.count());
  }
  @Test void brokerFailureBacksOffAndBlocksOnlyItsOwnAggregate() {
    var aggregate = UUID.randomUUID(); var event = UUID.randomUUID(); var head = enqueue(aggregate, event);
    var next = enqueue(aggregate, UUID.randomUUID()); var other = enqueue(UUID.randomUUID(), UUID.randomUUID());
    doThrow(new IllegalStateException("Broker down")).when(transport).send(any());
    relay.publishOne(head.getId());
    var failed = outbox.findById(head.getId()).orElseThrow();
    assertNull(failed.getPublishedAt()); assertEquals(1, failed.getAttempts());
    assertNotNull(failed.getLastError()); assertTrue(failed.getNextAttemptAt().isAfter(failed.getCreatedAt()));
    assertFalse(outbox.ready(Instant.now(), PageRequest.of(0, 50)).contains(next.getId()));
    doNothing().when(transport).send(any()); relay.publishOne(other.getId());
    assertNotNull(outbox.findById(other.getId()).orElseThrow().getPublishedAt());
    // Move the retry deadline into the past without waiting or running Kafka.
    tx().executeWithoutResult(status -> outbox.lock(head.getId()).orElseThrow()
        .failed(Instant.now().minusSeconds(120), new IllegalStateException("Test retry")));
    relay.publishOne(head.getId()); relay.publishOne(next.getId());
    assertNotNull(outbox.findById(head.getId()).orElseThrow().getPublishedAt());
    assertNotNull(outbox.findById(next.getId()).orElseThrow().getPublishedAt());
  }
  @Test void concurrentRelaysSendAnAcknowledgedRowOnlyOnce() throws Exception {
    var row = enqueue(UUID.randomUUID(), UUID.randomUUID());
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first = pool.submit(() -> relay.publishOne(row.getId()));
      var second = pool.submit(() -> relay.publishOne(row.getId()));
      first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
    }
    verify(transport, times(1)).send(any());
  }
  @Test void brokerAckBeforeDatabaseRollbackReplaysTheSameIdAndPayload() {
    var row = enqueue(UUID.randomUUID(), UUID.randomUUID());
    tx().executeWithoutResult(status -> {
      var locked = outbox.lock(row.getId()).orElseThrow(); transport.send(locked);
      locked.published(Instant.now()); status.setRollbackOnly();
    });
    assertNull(outbox.findById(row.getId()).orElseThrow().getPublishedAt());
    relay.publishOne(row.getId());
    var sent = org.mockito.ArgumentCaptor.forClass(OutboxMessage.class);
    verify(transport, times(2)).send(sent.capture());
    assertEquals(sent.getAllValues().get(0).getEventId(), sent.getAllValues().get(1).getEventId());
    assertEquals(sent.getAllValues().get(0).getPayload(), sent.getAllValues().get(1).getPayload());
  }
  @Test void inboxBusinessWriteAndGeneratedOutboxCommitTogetherWithCausationMetadata() {
    var incoming = UUID.randomUUID(); var outgoing = UUID.randomUUID(); var id = UUID.randomUUID();
    var headers = org.springframework.messaging.support.MessageBuilder.withPayload("payload")
        .setHeader("correlationId", "workflow-123").setHeader("schemaVersion", 1).build().getHeaders();
    assertThrows(IllegalStateException.class, () -> inbox.process("outbox.test", incoming, id, headers, () -> {
      business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null)); writer.enqueue("test-out-0", outgoing, id, "payload");
      throw new IllegalStateException("After enqueue");
    }));
    assertFalse(business.existsById(id)); assertTrue(outbox.findByEventId(outgoing).isEmpty());
    inbox.process("outbox.test", incoming, id, headers, () -> {
      business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null)); writer.enqueue("test-out-0", outgoing, id, "payload");
    });
    inbox.process("outbox.test", incoming, id, headers, () -> fail("Duplicate must be skipped"));
    var result = outbox.findByEventId(outgoing).orElseThrow();
    assertEquals(incoming.toString(), result.getCausationId()); assertEquals("workflow-123", result.getCorrelationId());
    assertEquals(id.toString(), EventContext.current(id).correlationId());
  }
}
