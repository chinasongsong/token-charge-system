package com.tokenhub.usercenter.domain.auth;

public interface PasswordHasher {

  String encode(String rawPassword);

  boolean matches(String rawPassword, String encoded);
}
