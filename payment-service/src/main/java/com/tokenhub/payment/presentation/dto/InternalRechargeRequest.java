package com.tokenhub.payment.presentation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InternalRechargeRequest(
    @NotNull Long userId,
    @NotNull @Positive Long amount,
    /** optional idempotency: duplicate requests return same logical result by skipping second credit */
    String idempotencyKey
) {}
