package com.tokenhub.adapter.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.tokenhub.adapter.application.ChatCompletionApplicationService;
import com.tokenhub.adapter.application.IdempotentChatCompletionApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenAiCompatibleController {

  private final ChatCompletionApplicationService chatCompletionApplicationService;
  private final IdempotentChatCompletionApplicationService idempotentChatCompletionApplicationService;

  public OpenAiCompatibleController(
      ChatCompletionApplicationService chatCompletionApplicationService,
      IdempotentChatCompletionApplicationService idempotentChatCompletionApplicationService
  ) {
    this.chatCompletionApplicationService = chatCompletionApplicationService;
    this.idempotentChatCompletionApplicationService = idempotentChatCompletionApplicationService;
  }

  @PostMapping(value = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode chatCompletions(@RequestBody JsonNode body, HttpServletRequest request) {
    return idempotentChatCompletionApplicationService.complete(body, request);
  }

  @GetMapping(value = "/v1/models", produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode listModels() {
    return chatCompletionApplicationService.models();
  }
}
