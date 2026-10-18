package com.dispatchai.dispatch.infrastructure.messaging.inbox;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.annotation.*;
import com.dispatchai.dispatch.domain.model.DeliveryAssignment;
import com.dispatchai.dispatch.infrastructure.persistence.repository.DeliveryAssignmentJpaRepository;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import(InboxProcessor.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InboxIntegrationTest {
  @Autowired InboxProcessor inbox;
  @Autowired DeliveryAssignmentJpaRepository business;
  private void process(String consumer, UUID eventId, UUID aggregateId, Runnable handler) {
    inbox.process(consumer, eventId, aggregateId, MessageBuilder.withPayload("payload").build().getHeaders(), handler);
  }
  @Test void rollbackDoesNotConsumeTheEventAndRetryCommitsExactlyOneBusinessChange() {
    var id = UUID.randomUUID(); var eventId = UUID.randomUUID();
    assertThrows(IllegalStateException.class, () -> process("test.rollback", eventId, id, () -> {
      business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null));
      throw new IllegalStateException("Failure after business write");
    }));
    assertFalse(business.existsById(id));
    process("test.rollback", eventId, id, () -> business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null)));
    process("test.rollback", eventId, id, () -> fail("Committed event must not execute again"));
    assertTrue(business.existsById(id));
  }
  @Test void concurrentDuplicatesRunTheHandlerOnce() throws Exception {
    var id = UUID.randomUUID(); var eventId = UUID.randomUUID(); var calls = new AtomicInteger();
    var start = new CountDownLatch(1);
    Runnable handler = () -> { calls.incrementAndGet(); business.saveAndFlush(new DeliveryAssignment(id, UUID.randomUUID(), UUID.randomUUID(), null)); };
    try (var pool = Executors.newFixedThreadPool(2)) {
      var first = pool.submit(() -> { await(start); process("test.concurrent", eventId, id, handler); });
      var second = pool.submit(() -> { await(start); process("test.concurrent", eventId, id, handler); });
      start.countDown(); first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
    }
    assertEquals(1, calls.get()); assertTrue(business.existsById(id));
  }
  @Test void identityIsScopedToTheConsumer() {
    var calls = new AtomicInteger(); var eventId = UUID.randomUUID(); var aggregate = UUID.randomUUID();
    process("test.one", eventId, aggregate, calls::incrementAndGet);
    process("test.two", eventId, aggregate, calls::incrementAndGet);
    process("test.one", eventId, aggregate, calls::incrementAndGet);
    assertEquals(2, calls.get());
  }
  @Test void unknownSchemaIsRejectedBeforeBusinessChanges() {
    var calls = new AtomicInteger();
    var headers = MessageBuilder.withPayload("payload").setHeader("schemaVersion", 99).build().getHeaders();
    assertThrows(IllegalArgumentException.class, () -> inbox.process("test.schema", UUID.randomUUID(),
        UUID.randomUUID(), headers, calls::incrementAndGet));
    assertEquals(0, calls.get());
  }
  private static void await(CountDownLatch latch) {
    try { if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out"); }
    catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
  }
}
