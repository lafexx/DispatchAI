package com.dispatchai.analytics.infrastructure.messaging.streams;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.util.Properties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.springframework.kafka.support.serializer.JacksonJsonSerde;
import com.dispatchai.analytics.domain.model.DeliveryStateUpdate;
import com.dispatchai.analytics.domain.model.AnalyticsDeliveryStatus;
import com.dispatchai.analytics.domain.model.WindowActivity;

class TimeAnalyticsTopologyTest {
  private DeliveryStateUpdate event(UUID id, AnalyticsDeliveryStatus status, boolean delayed, Instant time) {
    return new DeliveryStateUpdate(id, null, null, status, delayed, null, null, time, UUID.randomUUID());
  }
  @Test void distinctDeliveryRateBoundaryGraceAndThroughput() {
    var builder = new StreamsBuilder();
    var serde = new JacksonJsonSerde<>(DeliveryStateUpdate.class);
    TimeAnalytics.build(builder.stream("updates", Consumed.with(Serdes.String(), serde)));
    var properties = new Properties();
    properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "time-analytics-test");
    properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:9092");
    try (var driver = new TopologyTestDriver(builder.build(), properties)) {
      var input = driver.createInputTopic("updates", Serdes.String().serializer(), serde.serializer());
      var id = UUID.randomUUID(); var other = UUID.randomUUID();
      Instant base = Instant.parse("2026-10-13T10:00:00Z");
      input.pipeInput(id.toString(), event(id, AnalyticsDeliveryStatus.PENDING, false, base), base);
      input.pipeInput(id.toString(), event(id, null, true, base.plusSeconds(30)), base.plusSeconds(30));
      input.pipeInput(id.toString(), event(id, null, true, base.plusSeconds(40)), base.plusSeconds(40));
      input.pipeInput(other.toString(), event(other, AnalyticsDeliveryStatus.COMPLETED, false, base.plusSeconds(50)), base.plusSeconds(50));
      var delay = driver.<String, WindowActivity>getKeyValueStore("delay-history-store");
      var throughput = driver.<String, WindowActivity>getKeyValueStore("throughput-history-store");
      assertEquals(new WindowActivity(2,1,1,1), delay.get(Long.toString(base.toEpochMilli())));
      assertEquals(1, throughput.get(Long.toString(base.toEpochMilli())).created());
      // Event exactly at minute boundary belongs to the next throughput bucket.
      input.pipeInput(other.toString(), event(other, AnalyticsDeliveryStatus.PENDING, false, base.plusSeconds(60)), base.plusSeconds(60));
      assertEquals(1, throughput.get(Long.toString(base.plusSeconds(60).toEpochMilli())).created());
      // Advance stream time; an event within grace can still amend the old minute.
      input.pipeInput(other.toString(), event(other, null, false, base.plusSeconds(120)), base.plusSeconds(120));
      var late = UUID.randomUUID();
      input.pipeInput(late.toString(), event(late, AnalyticsDeliveryStatus.PENDING, false, base.plusSeconds(20)), base.plusSeconds(20));
      assertEquals(2, throughput.get(Long.toString(base.toEpochMilli())).created());
      input.pipeInput(other.toString(), event(other, null, false, base.plusSeconds(600)), base.plusSeconds(600));
      var tooLate = UUID.randomUUID();
      input.pipeInput(tooLate.toString(), event(tooLate, AnalyticsDeliveryStatus.PENDING, false, base.plusSeconds(10)), base.plusSeconds(10));
      assertEquals(2, throughput.get(Long.toString(base.toEpochMilli())).created());
    }
  }
}
