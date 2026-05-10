package com.tokenhub.usercenter.application;

import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.common.security.apikey.ApiKeySupport;
import com.tokenhub.usercenter.application.port.AccessTokenIssuer;
import com.tokenhub.usercenter.application.port.VerificationMailPort;
import com.tokenhub.usercenter.domain.auth.PasswordHasher;
import com.tokenhub.usercenter.domain.auth.PasswordResetRepository;
import com.tokenhub.usercenter.domain.user.UserAccount;
import com.tokenhub.usercenter.domain.user.UserDeviceRepository;
import com.tokenhub.usercenter.domain.user.UserRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserApplicationService {

  private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
  private static final int MIN_PASSWORD_LEN = 8;
  private static final int RESET_CODE_LEN = 6;
  private static final int RESET_TTL_MINUTES = 30;

  private final UserRepository users;
  private final UserDeviceRepository devices;
  private final PasswordHasher passwordHasher;
  private final PasswordResetRepository passwordResets;
  private final AccessTokenIssuer accessTokenIssuer;
  private final VerificationMailPort verificationMailPort;
  private final SecureRandom random = new SecureRandom();

  public UserApplicationService(
      UserRepository users,
      UserDeviceRepository devices,
      PasswordHasher passwordHasher,
      PasswordResetRepository passwordResets,
      AccessTokenIssuer accessTokenIssuer,
      VerificationMailPort verificationMailPort
  ) {
    this.users = users;
    this.devices = devices;
    this.passwordHasher = passwordHasher;
    this.passwordResets = passwordResets;
    this.accessTokenIssuer = accessTokenIssuer;
    this.verificationMailPort = verificationMailPort;
  }

  public UserAccount register(String email, String rawPassword, String displayName) {
    email = normalizeEmail(email);
    requireEmail(email);
    requirePassword(rawPassword);
    if (users.findByEmail(email).isPresent()) {
      throw new BusinessException(ErrorCode.CONFLICT, "邮箱已注册");
    }
    String hash = passwordHasher.encode(rawPassword);
    UserAccount draft = UserAccount.registered(email, hash, blankToNull(displayName));
    return users.save(draft);
  }

  public LoginResult login(String email, String rawPassword, String fingerprint, String userAgent, String ip) {
    email = normalizeEmail(email);
    requireEmail(email);
    UserAccount user = users.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误"));
    if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED, "邮箱或密码错误");
    }
    devices.recordLogin(user.getId(), blankToNull(fingerprint), blankToNull(userAgent), blankToNull(ip));
    String token = accessTokenIssuer.issueForUser(user.getId());
    return new LoginResult(token, user, accessTokenIssuer.accessTokenTtlSeconds());
  }

  public void requestPasswordReset(String email) {
    email = normalizeEmail(email);
    requireEmail(email);
    Optional<UserAccount> user = users.findByEmail(email);
    if (user.isEmpty()) {
      // Do not reveal existence
      return;
    }
    String code = String.format("%0" + RESET_CODE_LEN + "d", random.nextInt(1_000_000));
    String hash = ApiKeySupport.sha256HexUtf8(code);
    Instant expires = Instant.now().plus(RESET_TTL_MINUTES, ChronoUnit.MINUTES);
    passwordResets.savePendingCode(email, hash, expires);
    verificationMailPort.sendLoginOrVerificationHint(
        email,
        "密码重置验证码",
        "验证码（仅开发环境会出现在日志）: " + code + "，" + RESET_TTL_MINUTES + " 分钟内有效。"
    );
  }

  public void resetPassword(String email, String code, String newRawPassword) {
    email = normalizeEmail(email);
    requireEmail(email);
    requirePassword(newRawPassword);
    UserAccount user = users.findByEmail(email)
        .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "验证码无效或已过期"));
    boolean ok = passwordResets.tryConsumeLatest(email, code, Instant.now());
    if (!ok) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码无效或已过期");
    }
    users.updatePasswordHash(user.getId(), passwordHasher.encode(newRawPassword));
  }

  public UserProfile getProfile(long userId) {
    UserAccount user = users.findById(userId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "用户不存在"));
    return new UserProfile(user.getId(), user.getEmail(), user.getDisplayName());
  }

  private static void requireEmail(String email) {
    if (email == null || email.isBlank() || !EMAIL.matcher(email).matches()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "邮箱格式不正确");
    }
  }

  private static String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private static void requirePassword(String raw) {
    if (raw == null || raw.length() < MIN_PASSWORD_LEN) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "密码长度至少 " + MIN_PASSWORD_LEN + " 位");
    }
  }

  private static String blankToNull(String s) {
    if (s == null || s.isBlank()) {
      return null;
    }
    return s.trim();
  }

  public record LoginResult(String accessToken, UserAccount user, long expiresInSeconds) {}

  public record UserProfile(long id, String email, String displayName) {}
}
