package com.tokenhub.usercenter.infrastructure.security;

import com.tokenhub.common.security.apikey.ApiKeySupport;
import com.tokenhub.common.security.jwt.JwtSupport;
import io.jsonwebtoken.Claims;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JwtPrincipalResolver {

  private final JwtAccessTokenIssuer issuer;

  public JwtPrincipalResolver(JwtAccessTokenIssuer issuer) {
    this.issuer = issuer;
  }

  public Optional<Long> resolveBearer(String authorizationHeader) {
    String raw = ApiKeySupport.extractBearer(authorizationHeader);
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }
    try {
      Claims claims = JwtSupport.parse(raw, issuer.signingKey());
      return Optional.of(Long.parseLong(claims.getSubject()));
    } catch (RuntimeException ex) {
      return Optional.empty();
    }
  }
}
