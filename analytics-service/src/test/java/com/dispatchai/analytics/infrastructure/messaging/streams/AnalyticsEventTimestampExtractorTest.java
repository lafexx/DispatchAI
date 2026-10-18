package com.dispatchai.analytics.infrastructure.messaging.streams;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
class AnalyticsEventTimestampExtractorTest {
  @Test void timestampComesFromPayloadRatherThanPublicationTime() {
    var payload = "{\"eventTime\":\"2026-10-13T10:00:00Z\"}".getBytes(StandardCharsets.UTF_8);
    ConsumerRecord<Object,Object> record = new ConsumerRecord<>("updates",0,0L,"id",payload);
    assertEquals(Instant.parse("2026-10-13T10:00:00Z").toEpochMilli(),new AnalyticsEventTimestampExtractor().extract(record,0));
  }
  @Test void missingTimestampFailsClosed() {
    ConsumerRecord<Object,Object> record = new ConsumerRecord<>("updates",0,0L,"id","{}");
    assertThrows(IllegalArgumentException.class,() -> new AnalyticsEventTimestampExtractor().extract(record,0));
  }
}
