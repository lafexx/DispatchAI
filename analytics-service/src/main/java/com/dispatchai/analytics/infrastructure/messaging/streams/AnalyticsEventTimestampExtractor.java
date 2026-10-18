package com.dispatchai.analytics.infrastructure.messaging.streams;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;
import com.dispatchai.analytics.domain.model.DeliveryStateUpdate;

/** Extract before task stream-time advances, including binder non-native JSON input. */
public class AnalyticsEventTimestampExtractor implements TimestampExtractor {
  private static final Pattern EVENT_TIME = Pattern.compile("\\\"eventTime\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
  @Override public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
    Object value = record.value();
    Instant time;
    if (value instanceof DeliveryStateUpdate update) time = update.eventTime();
    else {
      String json = value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(value);
      var matcher = EVENT_TIME.matcher(json);
      if (!matcher.find()) throw new IllegalArgumentException("Analytics JSON requires ISO eventTime");
      time = Instant.parse(matcher.group(1));
    }
    if (time == null || time.toEpochMilli() < 0) throw new IllegalArgumentException("Invalid analytics eventTime");
    return time.toEpochMilli();
  }
}
