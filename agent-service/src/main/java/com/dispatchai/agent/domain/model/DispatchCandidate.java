package com.dispatchai.agent.domain.model;

import com.dispatchai.agent.infrastructure.client.dto.DriverAnalyticsResponse;
import com.dispatchai.agent.infrastructure.client.dto.DriverResponse;

public record DispatchCandidate(
    DriverResponse driver,
    DriverAnalyticsResponse analytics) {
}
