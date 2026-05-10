package com.tokenhub.adapter.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenhub.adapter.application.ChatCompletionApplicationService;
import com.tokenhub.adapter.infrastructure.billing.BillingSettlementClient;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenAiCompatibleController {

  private final ChatCompletionApplicationService chatCompletionApplicationService;
  private final BillingSettlementClient billingSettlementClient;

  public OpenAiCompatibleController(
      ChatCompletionApplicationService chatCompletionApplicationService,
      BillingSettlementClient billingSettlementClient
  ) {
    this.chatCompletionApplicationService = chatCompletionApplicationService;
    this.billingSettlementClient = billingSettlementClient;
  }

  @PostMapping(value = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode chatCompletions(@RequestBody JsonNode body, HttpServletRequest request) {
    JsonNode response = chatCompletionApplicationService.chat(body);
    billingSettlementClient.trySettle(request, body, response);
    return response;
  }

  @GetMapping(value = "/v1/models", produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode listModels() {
    return chatCompletionApplicationService.models();
  }
}
