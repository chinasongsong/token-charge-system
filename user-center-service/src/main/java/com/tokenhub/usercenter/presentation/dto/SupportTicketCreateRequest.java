package com.tokenhub.usercenter.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportTicketCreateRequest(@NotBlank String title) {}
