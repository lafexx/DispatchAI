package com.dispatchai.customer.presentation.dto;

import java.util.UUID;

public record CustomerResponse(UUID id, String name, String email, String phone) {
}
