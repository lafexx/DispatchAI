package com.dispatchai.agent.presentation.dto;

public record AgentRequest(
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=4000) String message) {
}
