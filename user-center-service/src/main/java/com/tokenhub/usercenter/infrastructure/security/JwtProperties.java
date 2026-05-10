package com.tokenhub.usercenter.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tokenhub.jwt")
public class JwtProperties {

  /**
   * JWT issuer claim.
   */
  private String issuer = "user-center";

  /**
   * HS256 secret (UTF-8), at least 32 bytes — inject via env in every environment.
   */
  private String secret = "dev-only-change-me-32bytes-minimum!!";

  private long ttlSeconds = 86_400;

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public void setTtlSeconds(long ttlSeconds) {
    this.ttlSeconds = ttlSeconds;
  }
}
