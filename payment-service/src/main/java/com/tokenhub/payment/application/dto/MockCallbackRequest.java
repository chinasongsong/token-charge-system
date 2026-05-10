package com.tokenhub.payment.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Mock 渠道回调体；签名为对按键名字典序拼接后的 canonical 串做 HMAC-SHA256(hex)。
 * 例如 canonical：amount=100&orderNo=...&status=PAID&ts=...&userId=...
 */
public record MockCallbackRequest(
    @NotBlank String orderNo,
    @NotNull @Positive Long userId,
    @NotNull @Positive Long amount,
    @NotBlank String status,
    @NotNull Long ts,
    @NotBlank String signature
) {}
