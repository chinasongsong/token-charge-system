package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record InvoiceRequest(
    @NotBlank String orderRef,
    @Positive long amount,
    String currency
) {}
