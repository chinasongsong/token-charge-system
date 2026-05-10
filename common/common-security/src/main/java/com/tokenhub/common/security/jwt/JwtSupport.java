package com.tokenhub.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import javax.crypto.SecretKey;

/**
 * Stateless JWT primitives; callers supply secrets from environment or vault — never bake values here.
 */
public final class JwtSupport {

  private JwtSupport() {}

  public static SecretKey hmacShaKeyFromUtf8(String secretUtf8, int minimumBytes) {
    Objects.requireNonNull(secretUtf8, "secret");
    byte[] bytes = secretUtf8.getBytes(StandardCharsets.UTF_8);
    if (bytes.length < minimumBytes) {
      throw new IllegalArgumentException("JWT secret must be at least " + minimumBytes + " bytes");
    }
    return Keys.hmacShaKeyFor(bytes);
  }

  public static String mintAccessToken(String subject, String issuer, SecretKey signingKey, long ttlMillis) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(subject)
        .issuer(issuer)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusMillis(ttlMillis)))
        .signWith(signingKey)
        .compact();
  }

  public static Claims parse(String jwt, SecretKey signingKey) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(jwt).getPayload();
  }
}
