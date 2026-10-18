package com.dispatchai.dispatch.infrastructure.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.messaging.MessageHeaders;

/** Thread-bound metadata, scoped to synchronous inbox processing only. */
public final class EventContext {
  private static final ThreadLocal<Metadata> CURRENT = new ThreadLocal<>();
  private EventContext() {}
  public record Metadata(String correlationId, String causationId) {}
  public static Metadata current(UUID aggregateId) {
    var value = CURRENT.get();
    return value == null ? new Metadata(aggregateId.toString(), null) : value;
  }
  public static Metadata enter(UUID eventId, UUID aggregateId, MessageHeaders headers) {
    var version = text(headers.get("schemaVersion"));
    if (version != null && !"1".equals(version)) {
      throw new IllegalArgumentException("Unsupported event schema version: " + version);
    }
    var previous = CURRENT.get();
    var correlation = text(headers.get("correlationId"));
    CURRENT.set(new Metadata(correlation == null ? aggregateId.toString() : correlation, eventId.toString()));
    return previous;
  }
  public static void restore(Metadata previous) {
    if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
  }
  private static String text(Object value) {
    if (value instanceof byte[] bytes) return new String(bytes, StandardCharsets.UTF_8);
    return value == null ? null : value.toString();
  }
}
