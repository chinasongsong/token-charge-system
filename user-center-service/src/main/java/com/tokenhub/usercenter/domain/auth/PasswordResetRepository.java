package com.tokenhub.usercenter.domain.auth;

import java.time.Instant;

public interface PasswordResetRepository {

  void savePendingCode(String email, String codeHash, Instant expiresAt);

  /**
   * Marks the latest matching unconsumed code as consumed when the plain code matches.
   *
   * @return true when a valid row was consumed
   */
  boolean tryConsumeLatest(String email, String plainCode, Instant now);
}
