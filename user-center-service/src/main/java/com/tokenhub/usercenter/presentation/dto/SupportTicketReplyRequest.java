package com.tokenhub.usercenter.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketReplyRequest(
    @NotBlank @Size(max = 8000) String body
) {}
