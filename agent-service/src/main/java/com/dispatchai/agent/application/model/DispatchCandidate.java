package com.dispatchai.agent.application.model;

import com.dispatchai.agent.infrastructure.client.analytics.dto.DriverAnalyticsResponse;
import com.dispatchai.agent.infrastructure.client.fleet.dto.DriverResponse;

public record DispatchCandidate(
    DriverResponse driver,
    DriverAnalyticsResponse analytics) {
}
