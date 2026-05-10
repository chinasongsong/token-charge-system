package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RefundApplyRequest(
    @NotBlank String orderNo,
    @Positive long amount,
    String reason
) {}
