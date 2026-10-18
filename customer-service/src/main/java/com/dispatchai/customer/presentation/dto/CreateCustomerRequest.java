package com.dispatchai.customer.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
    @NotBlank String name,
    @Email @NotBlank String email,
    String phone) {
}
