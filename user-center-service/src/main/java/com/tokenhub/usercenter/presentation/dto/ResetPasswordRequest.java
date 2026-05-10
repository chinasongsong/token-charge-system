package com.tokenhub.usercenter.presentation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, max = 16) String code,
    @NotBlank @Size(min = 8, max = 128) String newPassword
) {}
