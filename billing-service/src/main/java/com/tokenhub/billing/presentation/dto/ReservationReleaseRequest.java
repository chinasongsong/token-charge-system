package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record ReservationReleaseRequest(@NotBlank String traceId) {}
