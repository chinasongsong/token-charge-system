package com.tokenhub.common.security.apikey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Utilities for platform API keys (`sk_*` naming used by callers). */
public final class ApiKeySupport {

  private static final int NORMALIZED_MIN_VISIBLE = 6;

  private ApiKeySupport() {}

  public static boolean looksLikeBearer(String headerValue) {
    return headerValue != null && headerValue.regionMatches(true, 0, "Bearer ", 0, 7);
  }

  /** Returns raw token substring from {@code Authorization} header for {@code Bearer } scheme. */
  public static String extractBearer(String authorizationHeader) {
    if (!looksLikeBearer(authorizationHeader)) {
      return null;
    }
    return authorizationHeader.substring(7).trim();
  }

  /**
   * Loose validation for MVP: non-blank printable ASCII with reasonable entropy floor.
   */
  public static boolean isPlausibleSecret(String rawSecret) {
    if (rawSecret == null || rawSecret.isBlank()) {
      return false;
    }
    return rawSecret.length() >= NORMALIZED_MIN_VISIBLE;
  }

  /** SHA-256 fingerprint (hex lowercase) suitable for lookups without storing raw secret. */
  public static String sha256HexUtf8(String raw) {
    Objects.requireNonNull(raw, "raw");
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
    byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hashed);
  }

  public static String maskForLogging(String raw) {
    if (raw == null || raw.length() <= 4) {
      return "****";
    }
    return raw.substring(0, 2) + "***" + raw.substring(raw.length() - 2);
  }
}
