package com.tokenhub.usercenter.application.dto;

import java.time.LocalDateTime;

public record SupportTicketItem(
    long id,
    String title,
    String status,
    String priority,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
