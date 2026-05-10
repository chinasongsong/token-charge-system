package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.NotNull;

public record PreflightRequest(@NotNull Long userId) {}
