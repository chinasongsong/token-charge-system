package com.tokenhub.billing.presentation;

import com.tokenhub.billing.application.ApiKeyApplicationService;
import com.tokenhub.billing.infrastructure.persistence.ApiKeyPo;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/api-keys")
public class InternalApiKeyController {

  private final ApiKeyApplicationService apiKeyApplicationService;

  public InternalApiKeyController(ApiKeyApplicationService apiKeyApplicationService) {
    this.apiKeyApplicationService = apiKeyApplicationService;
  }

  @GetMapping("/by-fingerprint/{fingerprint}")
  public ResponseEntity<ApiKeyResolution> byFingerprint(@PathVariable String fingerprint) {
    Optional<ApiKeyPo> row = apiKeyApplicationService.findActiveByFingerprint(fingerprint);
    return row.map(r -> ResponseEntity.ok(new ApiKeyResolution(r.getUserId(), r.getId())))
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  public record ApiKeyResolution(long userId, long apiKeyId) {}
}
