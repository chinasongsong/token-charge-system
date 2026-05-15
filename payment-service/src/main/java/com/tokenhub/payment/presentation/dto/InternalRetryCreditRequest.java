package com.tokenhub.payment.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record InternalRetryCreditRequest(@NotBlank String orderNo) {}
