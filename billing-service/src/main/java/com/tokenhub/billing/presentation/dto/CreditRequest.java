package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreditRequest(
    @NotNull @Positive Long amount,
    @NotNull Long userId,
    @NotBlank String sourceRef
) {}
