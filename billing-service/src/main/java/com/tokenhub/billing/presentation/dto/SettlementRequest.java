package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SettlementRequest(
    @NotBlank String traceId,
    @NotNull Long userId,
    Long apiKeyId,
    @NotBlank String providerCode,
    @NotBlank String modelName,
    long inputTokens,
    long outputTokens
) {}
