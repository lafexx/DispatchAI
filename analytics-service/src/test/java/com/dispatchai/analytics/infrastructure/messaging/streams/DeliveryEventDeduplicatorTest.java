package com.dispatchai.analytics.infrastructure.messaging.streams;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.apache.kafka.streams.processor.api.*;
import org.apache.kafka.streams.state.KeyValueStore;
import com.dispatchai.analytics.domain.model.DeliveryStateUpdate;
import com.dispatchai.analytics.domain.model.AnalyticsDeliveryStatus;

@SuppressWarnings("unchecked")
class DeliveryEventDeduplicatorTest {
  private FixedKeyProcessorContext<String, DeliveryStateUpdate> context;
  private KeyValueStore<String, Boolean> store;
  private final Map<String, Boolean> state = new HashMap<>();
  private DeliveryEventDeduplicator processor;
  @BeforeEach void setup() {
    context = mock(FixedKeyProcessorContext.class); store = mock(KeyValueStore.class);
    when(context.getStateStore(DeliveryEventDeduplicator.STORE)).thenReturn(store);
    when(store.get(anyString())).thenAnswer(call -> state.get(call.getArgument(0)));
    doAnswer(call -> { state.put(call.getArgument(0), call.getArgument(1)); return null; })
        .when(store).put(anyString(), anyBoolean());
    processor = new DeliveryEventDeduplicator(); processor.init(context);
  }
  private FixedKeyRecord<String, DeliveryStateUpdate> record(UUID delivery, UUID eventId) {
    var record = (FixedKeyRecord<String, DeliveryStateUpdate>) mock(FixedKeyRecord.class);
    when(record.key()).thenReturn(delivery.toString());
    when(record.value()).thenReturn(new DeliveryStateUpdate(delivery, UUID.randomUUID(), null,
        AnalyticsDeliveryStatus.PENDING, false, null, null, Instant.now(), eventId));
    when(record.withTimestamp(anyLong())).thenReturn(record);
    return record;
  }
  @Test void stableIdDuplicatesDoNotReachTheProjectionEvenAfterProcessorRecreation() {
    var record = record(UUID.randomUUID(), UUID.randomUUID());
    processor.process(record); processor.process(record);
    var restarted = new DeliveryEventDeduplicator(); restarted.init(context); restarted.process(record);
    verify(context, times(1)).forward(record);
  }
  @Test void distinctEventsForTheSameDeliveryAreBothForwarded() {
    var delivery = UUID.randomUUID(); var first = record(delivery, UUID.randomUUID());
    var second = record(delivery, UUID.randomUUID());
    processor.process(first); processor.process(second); verify(context).forward(first); verify(context).forward(second);
  }
  @Test void missingIdentityFailsClosed() {
    assertThrows(IllegalArgumentException.class, () -> processor.process(record(UUID.randomUUID(), null)));
    verify(context, never()).forward(any(FixedKeyRecord.class));
  }
  @Test void wrongPartitionKeyFailsBeforeRecordingTheEvent() {
    var record = record(UUID.randomUUID(), UUID.randomUUID());
    when(record.key()).thenReturn("wrong-key");
    assertThrows(IllegalArgumentException.class, () -> processor.process(record)); verify(store, never()).put(any(), any());
  }
}
