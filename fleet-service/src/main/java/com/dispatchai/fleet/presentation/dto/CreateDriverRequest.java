package com.dispatchai.fleet.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDriverRequest(@NotBlank String name) {
}
