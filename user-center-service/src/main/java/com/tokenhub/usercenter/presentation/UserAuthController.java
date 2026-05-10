package com.tokenhub.usercenter.presentation;

import com.tokenhub.common.core.api.ApiResponse;
import com.tokenhub.usercenter.application.UserApplicationService;
import com.tokenhub.usercenter.domain.auth.AuthConstants;
import com.tokenhub.usercenter.presentation.dto.ForgotPasswordRequest;
import com.tokenhub.usercenter.presentation.dto.LoginRequest;
import com.tokenhub.usercenter.presentation.dto.RegisterRequest;
import com.tokenhub.usercenter.presentation.dto.ResetPasswordRequest;
import com.tokenhub.usercenter.presentation.dto.TokenResponse;
import com.tokenhub.usercenter.presentation.dto.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Validated
public class UserAuthController {

  private final UserApplicationService userApplicationService;

  public UserAuthController(UserApplicationService userApplicationService) {
    this.userApplicationService = userApplicationService;
  }

  @PostMapping("/register")
  public ApiResponse<UserProfileResponse> register(@Valid @RequestBody RegisterRequest request) {
    var user = userApplicationService.register(request.email(), request.password(), request.displayName());
    return ApiResponse.ok(new UserProfileResponse(user.getId(), user.getEmail(), user.getDisplayName()));
  }

  @PostMapping("/login")
  public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    String fp = http.getHeader("X-Device-Fingerprint");
    String ip = clientIp(http);
    var result = userApplicationService.login(
        request.email(),
        request.password(),
        fp,
        http.getHeader("User-Agent"),
        ip
    );
    return ApiResponse.ok(
        new TokenResponse(result.accessToken(), "Bearer", result.expiresInSeconds())
    );
  }

  @PostMapping("/logout")
  public ApiResponse<Void> logout() {
    return ApiResponse.ok();
  }

  @PostMapping("/password/forgot")
  public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
    userApplicationService.requestPasswordReset(request.email());
    return ApiResponse.ok();
  }

  @PostMapping("/password/reset")
  public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest request) {
    userApplicationService.resetPassword(request.email(), request.code(), request.newPassword());
    return ApiResponse.ok();
  }

  @GetMapping("/me")
  public ApiResponse<UserProfileResponse> me(HttpServletRequest http) {
    Long userId = (Long) http.getAttribute(AuthConstants.REQUEST_USER_ID);
    var profile = userApplicationService.getProfile(userId);
    return ApiResponse.ok(new UserProfileResponse(profile.id(), profile.email(), profile.displayName()));
  }

  private static String clientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      return forwarded.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
