package com.tokenhub.usercenter.infrastructure.security;

import com.tokenhub.usercenter.domain.auth.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasher implements PasswordHasher {

  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

  @Override
  public String encode(String rawPassword) {
    return encoder.encode(rawPassword);
  }

  @Override
  public boolean matches(String rawPassword, String encoded) {
    return encoder.matches(rawPassword, encoded);
  }
}
