package com.tokenhub.usercenter.domain.user;

import java.util.Objects;

/**
 * Aggregate root for authentication (no persistence annotations).
 */
public final class UserAccount {

  private final Long id;
  private final String email;
  private final String passwordHash;
  private final String displayName;
  private final String status;

  public UserAccount(Long id, String email, String passwordHash, String displayName, String status) {
    this.id = id;
    this.email = Objects.requireNonNull(email, "email");
    this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    this.displayName = displayName;
    this.status = status != null ? status : "ACTIVE";
  }

  public static UserAccount registered(String email, String passwordHash, String displayName) {
    return new UserAccount(null, email, passwordHash, displayName, "ACTIVE");
  }

  public UserAccount withId(long newId) {
    return new UserAccount(newId, email, passwordHash, displayName, status);
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getStatus() {
    return status;
  }
}
