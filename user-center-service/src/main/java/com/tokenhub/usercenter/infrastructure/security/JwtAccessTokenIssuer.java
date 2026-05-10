package com.tokenhub.usercenter.infrastructure.security;

import com.tokenhub.common.security.jwt.JwtSupport;
import com.tokenhub.usercenter.application.port.AccessTokenIssuer;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

  private final JwtProperties properties;
  private SecretKey signingKey;

  public JwtAccessTokenIssuer(JwtProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void initSigningKey() {
    String secret = Objects.requireNonNull(properties.getSecret(), "tokenhub.jwt.secret");
    this.signingKey = JwtSupport.hmacShaKeyFromUtf8(secret, 32);
  }

  @Override
  public String issueForUser(long userId) {
    return JwtSupport.mintAccessToken(
        String.valueOf(userId),
        properties.getIssuer(),
        signingKey,
        properties.getTtlSeconds() * 1000L
    );
  }

  @Override
  public long accessTokenTtlSeconds() {
    return properties.getTtlSeconds();
  }

  public SecretKey signingKey() {
    return signingKey;
  }

  public JwtProperties properties() {
    return properties;
  }
}
