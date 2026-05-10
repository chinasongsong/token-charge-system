package com.tokenhub.billing.presentation.dto;

import jakarta.validation.constraints.Size;

public record CreateApiKeyRequest(@Size(max = 191) String name) {}
