package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.ApiKeyApplicationService;
import com.tokenhub.billing.domain.auth.BillingAuthConstants;
import com.tokenhub.billing.infrastructure.persistence.ApiKeyPo;
import com.tokenhub.billing.presentation.dto.CreateApiKeyRequest;
import com.tokenhub.billing.presentation.dto.PatchApiKeyRequest;
import com.tokenhub.common.core.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apikeys")
@Validated
public class ApiKeyController {

  private final ApiKeyApplicationService apiKeyApplicationService;

  public ApiKeyController(ApiKeyApplicationService apiKeyApplicationService) {
    this.apiKeyApplicationService = apiKeyApplicationService;
  }

  @PostMapping
  public ApiResponse<ApiKeyApplicationService.CreatedApiKey> create(
      @Valid @RequestBody(required = false) CreateApiKeyRequest request,
      HttpServletRequest http
  ) {
    Long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    String name = request != null ? request.name() : null;
    return ApiResponse.ok(apiKeyApplicationService.create(userId, name));
  }

  @GetMapping
  public ApiResponse<List<ApiKeyView>> list(HttpServletRequest http) {
    Long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    List<ApiKeyPo> rows = apiKeyApplicationService.listForUser(userId);
    List<ApiKeyView> views = rows.stream()
        .map(r -> new ApiKeyView(r.getId(), r.getName(), r.getStatus(), r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""))
        .toList();
    return ApiResponse.ok(views);
  }

  @PatchMapping("/{id}")
  public ApiResponse<Void> patch(
      @PathVariable("id") long id,
      @Valid @RequestBody PatchApiKeyRequest request,
      HttpServletRequest http
  ) {
    Long userId = (Long) http.getAttribute(BillingAuthConstants.REQUEST_USER_ID);
    if ("DISABLED".equalsIgnoreCase(request.status())) {
      apiKeyApplicationService.disable(userId, id);
    }
    return ApiResponse.ok();
  }

  public record ApiKeyView(long id, String name, String status, String createdAt) {}
}
