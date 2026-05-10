package com.tokenhub.billing.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class PaymentRechargeClient {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  @Value("${tokenhub.payment.base-url:http://127.0.0.1:8104}")
  private String paymentBaseUrl;

  @Value("${BILLING_INTERNAL_TOKEN:dev-internal-token}")
  private String internalToken;

  public PaymentRechargeClient(
      @Qualifier("billingRestTemplate") RestTemplate billingRestTemplate,
      ObjectMapper objectMapper
  ) {
    this.restTemplate = billingRestTemplate;
    this.objectMapper = objectMapper;
  }

  public record PaidOrder(String orderNo, long amount, String currency, String status) {}

  public PaidOrder rechargeViaPayment(long userId, long amount) {
    String url = trimSlash(paymentBaseUrl) + "/internal/payments/recharge";
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("userId", userId);
    body.put("amount", amount);
    body.put("idempotencyKey", "recharge-" + userId + "-" + UUID.randomUUID());
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Token", internalToken);
    String raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
    try {
      JsonNode root = objectMapper.readTree(raw);
      JsonNode data = root.get("data");
      return new PaidOrder(
          data.get("orderNo").asText(),
          data.get("amount").asLong(),
          data.get("currency").asText(),
          data.get("status").asText()
      );
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.INTERNAL, "payment response parse failed: " + raw, e);
    }
  }

  private static String trimSlash(String s) {
    if (s == null || s.isBlank()) {
      return "";
    }
    String t = s.trim();
    while (t.endsWith("/")) {
      t = t.substring(0, t.length() - 1);
    }
    return t;
  }
}
