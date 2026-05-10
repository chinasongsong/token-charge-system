package com.tokenhub.usercenter.application.dto;

import java.time.LocalDateTime;

public record SupportTicketMessageItem(
    long id,
    long ticketId,
    long userId,
    String role,
    String body,
    LocalDateTime createdAt
) {}
