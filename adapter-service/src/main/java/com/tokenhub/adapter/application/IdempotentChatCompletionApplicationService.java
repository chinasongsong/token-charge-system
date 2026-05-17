package com.tokenhub.adapter.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenhub.adapter.infrastructure.billing.BillingSettlementClient;
import com.tokenhub.adapter.infrastructure.cache.RedisChatIdempotencyResponseCache;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Chat 完成编排：在存在网关复合幂等键时，用 Redis 缓存响应，避免重复调上游与重复结算。
 */
@Service
public class IdempotentChatCompletionApplicationService {

  private final ChatCompletionApplicationService chatCompletionApplicationService;
  private final BillingSettlementClient billingSettlementClient;
  private final RedisChatIdempotencyResponseCache idempotencyCache;

  public IdempotentChatCompletionApplicationService(
      ChatCompletionApplicationService chatCompletionApplicationService,
      BillingSettlementClient billingSettlementClient,
      RedisChatIdempotencyResponseCache idempotencyCache
  ) {
    this.chatCompletionApplicationService = chatCompletionApplicationService;
    this.billingSettlementClient = billingSettlementClient;
    this.idempotencyCache = idempotencyCache;
  }

  public JsonNode complete(JsonNode body, HttpServletRequest request) {
    String compositeKey = request.getHeader(BillingSettlementClient.HEADER_IDEMPOTENCY_COMPOSITE);
    if (!idempotencyCache.isActive() || compositeKey == null || compositeKey.isBlank()) {
      return executeWithoutCache(body, request);
    }

    String key = compositeKey.trim();
    Optional<JsonNode> cached = idempotencyCache.get(key);
    if (cached.isPresent()) {
      return cached.get();
    }

    if (!idempotencyCache.tryLock(key)) {
      Optional<JsonNode> waited = idempotencyCache.waitForValue(key);
      if (waited.isPresent()) {
        return waited.get();
      }
      throw new BusinessException(
          ErrorCode.CONFLICT,
          "相同幂等键的请求正在处理中，请稍后重试"
      );
    }

    try {
      cached = idempotencyCache.get(key);
      if (cached.isPresent()) {
        return cached.get();
      }
      JsonNode response = chatCompletionApplicationService.chat(body);
      boolean settled = billingSettlementClient.trySettle(request, body, response);
      if (RedisChatIdempotencyResponseCache.isCacheableResponse(response)
          && (settled || !billingSettlementClient.isSettlementEnabled())) {
        idempotencyCache.put(key, response);
      }
      return response;
    } finally {
      idempotencyCache.releaseLock(key);
    }
  }

  private JsonNode executeWithoutCache(JsonNode body, HttpServletRequest request) {
    JsonNode response = chatCompletionApplicationService.chat(body);
    billingSettlementClient.trySettle(request, body, response);
    return response;
  }
}
