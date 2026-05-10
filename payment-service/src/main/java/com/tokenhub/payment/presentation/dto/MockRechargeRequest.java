package com.tokenhub.payment.presentation.dto;

import jakarta.validation.constraints.Positive;

public record MockRechargeRequest(@Positive long amount) {}
