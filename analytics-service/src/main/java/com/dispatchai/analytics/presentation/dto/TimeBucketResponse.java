package com.dispatchai.analytics.presentation.dto;
import java.time.Instant;
/** Half-open UTC interval; delayRate is null when there is no activity. */
public record TimeBucketResponse(Instant start, Instant end, long created, long completed,
    long activeDeliveries, long delayedDeliveries, Double delayRate) {}
