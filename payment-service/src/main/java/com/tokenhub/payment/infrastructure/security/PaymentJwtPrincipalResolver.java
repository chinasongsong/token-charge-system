package com.tokenhub.payment.infrastructure.security;

import com.tokenhub.common.security.apikey.ApiKeySupport;
import com.tokenhub.common.security.jwt.JwtSupport;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentJwtPrincipalResolver {

  private final SecretKey signingKey;

  public PaymentJwtPrincipalResolver(
      @Value("${JWT_SECRET:dev-only-change-me-32bytes-minimum!!}") String jwtSecretUtf8
  ) {
    this.signingKey = JwtSupport.hmacShaKeyFromUtf8(jwtSecretUtf8, 32);
  }

  public Optional<Long> resolveBearer(String authorizationHeader) {
    String raw = ApiKeySupport.extractBearer(authorizationHeader);
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims = JwtSupport.parse(raw, signingKey);
      return Optional.of(Long.parseLong(claims.getSubject()));
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }
}
