package com.dispatchai.agent.domain.model;

public record DispatchAdviceDraft(
    String recommendedDriverId,
    String recommendedDriverName,
    String reasoning) {
}
