package com.tokenhub.payment.infrastructure.billing;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class BillingCreditClient {

  private final RestTemplate restTemplate;

  @Value("${tokenhub.billing.base-url:http://127.0.0.1:8103}")
  private String billingBaseUrl;

  @Value("${BILLING_INTERNAL_TOKEN:dev-internal-token}")
  private String internalToken;

  public BillingCreditClient(@Qualifier("paymentRestTemplate") RestTemplate paymentRestTemplate) {
    this.restTemplate = paymentRestTemplate;
  }

  public void creditBalance(long userId, long amount, String sourceRef) {
    String base = trimTrailingSlash(billingBaseUrl);
    String url = base + "/internal/billing/credit";
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("userId", userId);
    body.put("amount", amount);
    body.put("sourceRef", sourceRef);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("X-Internal-Token", internalToken);
    restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Void.class);
  }

  private static String trimTrailingSlash(String s) {
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
