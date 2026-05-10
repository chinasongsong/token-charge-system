package com.tokenhub.usercenter.presentation.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}
