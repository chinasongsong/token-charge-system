package com.tokenhub.usercenter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.usercenter.application.port.AccessTokenIssuer;
import com.tokenhub.usercenter.application.port.VerificationMailPort;
import com.tokenhub.usercenter.domain.auth.PasswordHasher;
import com.tokenhub.usercenter.domain.auth.PasswordResetRepository;
import com.tokenhub.usercenter.domain.user.UserAccount;
import com.tokenhub.usercenter.domain.user.UserDeviceRepository;
import com.tokenhub.usercenter.domain.user.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserApplicationServiceTest {

  @Mock
  private UserRepository users;
  @Mock
  private UserDeviceRepository devices;
  @Mock
  private PasswordHasher passwordHasher;
  @Mock
  private PasswordResetRepository passwordResets;
  @Mock
  private AccessTokenIssuer accessTokenIssuer;
  @Mock
  private VerificationMailPort verificationMailPort;

  private UserApplicationService service;

  @BeforeEach
  void setUp() {
    service = new UserApplicationService(
        users,
        devices,
        passwordHasher,
        passwordResets,
        accessTokenIssuer,
        verificationMailPort
    );
    when(accessTokenIssuer.accessTokenTtlSeconds()).thenReturn(3600L);
  }

  @Test
  void register_success() {
    when(users.findByEmail("a@b.com")).thenReturn(Optional.empty());
    when(passwordHasher.encode("password12")).thenReturn("HASH");
    when(users.save(any(UserAccount.class))).thenAnswer(inv -> ((UserAccount) inv.getArgument(0)).withId(9L));

    UserAccount saved = service.register("A@B.com", "password12", "nick");

    assertThat(saved.getId()).isEqualTo(9L);
    assertThat(saved.getEmail()).isEqualTo("a@b.com");
    verify(users).save(any(UserAccount.class));
  }

  @Test
  void register_conflict() {
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(new UserAccount(1L, "a@b.com", "x", null, "ACTIVE")));

    assertThatThrownBy(() -> service.register("a@b.com", "password12", null))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.CONFLICT);
  }

  @Test
  void register_invalid_email() {
    assertThatThrownBy(() -> service.register("not-an-email", "password12", null))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  void register_weak_password() {
    assertThatThrownBy(() -> service.register("a@b.com", "short", null))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  void login_success() {
    UserAccount user = new UserAccount(3L, "a@b.com", "ENC", null, "ACTIVE");
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordHasher.matches("password12", "ENC")).thenReturn(true);
    when(accessTokenIssuer.issueForUser(3L)).thenReturn("jwt-token");

    var result = service.login("a@b.com", "password12", "fp", "ua", "127.0.0.1");

    assertThat(result.accessToken()).isEqualTo("jwt-token");
    assertThat(result.user().getId()).isEqualTo(3L);
    assertThat(result.expiresInSeconds()).isEqualTo(3600L);
    verify(devices).recordLogin(3L, "fp", "ua", "127.0.0.1");
  }

  @Test
  void login_unknown_user() {
    when(users.findByEmail("a@b.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.login("a@b.com", "password12", null, null, null))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }

  @Test
  void login_bad_password() {
    UserAccount user = new UserAccount(3L, "a@b.com", "ENC", null, "ACTIVE");
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordHasher.matches("password12", "ENC")).thenReturn(false);

    assertThatThrownBy(() -> service.login("a@b.com", "password12", null, null, null))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.UNAUTHORIZED);
  }

  @Test
  void forgot_password_unknown_user_no_side_effect() {
    when(users.findByEmail("ghost@b.com")).thenReturn(Optional.empty());

    service.requestPasswordReset("ghost@b.com");

    verify(passwordResets, never()).savePendingCode(anyString(), anyString(), any(Instant.class));
    verify(verificationMailPort, never()).sendLoginOrVerificationHint(anyString(), anyString(), anyString());
  }

  @Test
  void forgot_password_known_user() {
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(new UserAccount(1L, "a@b.com", "x", null, "ACTIVE")));

    service.requestPasswordReset("a@b.com");

    verify(passwordResets).savePendingCode(eq("a@b.com"), anyString(), any(Instant.class));
    verify(verificationMailPort).sendLoginOrVerificationHint(eq("a@b.com"), anyString(), anyString());
  }

  @Test
  void reset_password_success() {
    UserAccount user = new UserAccount(2L, "a@b.com", "OLD", null, "ACTIVE");
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(user));
    when(passwordResets.tryConsumeLatest(eq("a@b.com"), eq("123456"), any(Instant.class))).thenReturn(true);
    when(passwordHasher.encode("newpassword12")).thenReturn("NEW");

    service.resetPassword("a@b.com", "123456", "newpassword12");

    verify(users).updatePasswordHash(2L, "NEW");
  }

  @Test
  void reset_password_user_missing() {
    when(users.findByEmail("a@b.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.resetPassword("a@b.com", "123456", "newpassword12"))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  void reset_password_code_invalid() {
    when(users.findByEmail("a@b.com")).thenReturn(Optional.of(new UserAccount(2L, "a@b.com", "OLD", null, "ACTIVE")));
    when(passwordResets.tryConsumeLatest(eq("a@b.com"), eq("000000"), any(Instant.class))).thenReturn(false);

    assertThatThrownBy(() -> service.resetPassword("a@b.com", "000000", "newpassword12"))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.BAD_REQUEST);
  }

  @Test
  void profile_found() {
    when(users.findById(5L)).thenReturn(Optional.of(new UserAccount(5L, "a@b.com", "x", "Nick", "ACTIVE")));

    var p = service.getProfile(5L);

    assertThat(p.id()).isEqualTo(5L);
    assertThat(p.email()).isEqualTo("a@b.com");
    assertThat(p.displayName()).isEqualTo("Nick");
  }

  @Test
  void profile_missing() {
    when(users.findById(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProfile(5L))
        .isInstanceOf(BusinessException.class)
        .extracting(ex -> ((BusinessException) ex).getErrorCode())
        .isEqualTo(ErrorCode.NOT_FOUND);
  }
}
