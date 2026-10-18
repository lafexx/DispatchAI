package com.dispatchai.delivery.infrastructure.messaging.outbox;

import com.dispatchai.delivery.infrastructure.persistence.entity.OutboxMessage;

import java.nio.charset.StandardCharsets;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class OutboxTransport {
  private final StreamBridge bridge;
  public OutboxTransport(StreamBridge bridge) { this.bridge = bridge; }
  public void send(OutboxMessage event) {
    var message = MessageBuilder.withPayload(event.getPayload().getBytes(StandardCharsets.UTF_8))
        .setHeader("contentType", "application/json")
        .setHeader("messageKey", event.getAggregateId().toString().getBytes(StandardCharsets.UTF_8))
        .setHeader("eventId", event.getEventId().toString())
        .setHeader("aggregateId", event.getAggregateId().toString())
        .setHeader("schemaVersion", event.getSchemaVersion())
        .setHeader("eventType", event.getEventType())
        .setHeader("eventSource", event.getSource())
        .setHeader("correlationId", event.getCorrelationId())
        .setHeader("causationId", event.getCausationId()).build();
    // The Kafka producer MUST remain synchronous (see application.properties).
    // false, exceptions and uncertain acknowledgements all retain the row.
    if (!bridge.send(event.getBindingName(), message)) {
      throw new IllegalStateException("Kafka binding rejected event " + event.getEventId());
    }
  }
}
