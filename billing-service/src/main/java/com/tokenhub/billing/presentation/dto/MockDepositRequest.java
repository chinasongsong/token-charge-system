package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.Positive;

public record MockDepositRequest(@Positive long amount) {}
