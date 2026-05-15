package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ReservationCommitRequest(
    @NotBlank String traceId,
    @PositiveOrZero long committedAmount
) {}
