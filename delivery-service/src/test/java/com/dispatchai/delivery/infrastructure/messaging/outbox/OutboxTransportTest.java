package com.dispatchai.delivery.infrastructure.messaging.outbox;

import com.dispatchai.delivery.infrastructure.persistence.entity.OutboxMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;

class OutboxTransportTest {
  @Test void falseSendIsAFailureAndPayloadAndMetadataArePreserved() {
    var bridge = mock(StreamBridge.class);
    var entry = new OutboxMessage(UUID.randomUUID(), UUID.randomUUID(), "test-out-0", "{\"value\":1}", "test-service");
    var transport = new OutboxTransport(bridge);
    when(bridge.send(eq("test-out-0"), any())).thenReturn(false);
    assertThrows(IllegalStateException.class, () -> transport.send(entry));
    var sent = ArgumentCaptor.forClass(Object.class); verify(bridge).send(eq("test-out-0"), sent.capture());
    var message = (Message<?>) sent.getValue();
    assertArrayEquals(entry.getPayload().getBytes(StandardCharsets.UTF_8), (byte[]) message.getPayload());
    assertEquals(entry.getEventId().toString(), message.getHeaders().get("eventId"));
    assertEquals(1, message.getHeaders().get("schemaVersion"));
    assertEquals(entry.getAggregateId().toString(), message.getHeaders().get("correlationId"));
  }
}
