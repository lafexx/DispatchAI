package com.dispatchai.delivery.presentation.dto;
public record SetParcelWeightRequest(@jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0.01") @jakarta.validation.constraints.Digits(integer=8,fraction=2)
    java.math.BigDecimal parcelWeightKg) {}
