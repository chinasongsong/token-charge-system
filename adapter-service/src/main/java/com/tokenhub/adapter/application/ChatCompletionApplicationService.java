package com.tokenhub.adapter.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenhub.adapter.domain.provider.ProviderAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatCompletionApplicationService {

  private final ProviderAdapter primaryAdapter;
  private final String defaultChatModel;

  public ChatCompletionApplicationService(
      ProviderAdapter primaryAdapter,
      @Value("${tokenhub.adapter.default-chat-model:deepseek-v4-flash}") String defaultChatModel
  ) {
    this.primaryAdapter = primaryAdapter;
    this.defaultChatModel = defaultChatModel;
  }

  public JsonNode chat(JsonNode body) {
    JsonNode mutable = body.deepCopy();
    if (!mutable.hasNonNull("model") || mutable.get("model").asText().isBlank()) {
      ((ObjectNode) mutable).put("model", defaultChatModel);
    }
    return primaryAdapter.chat(mutable);
  }

  public JsonNode models() {
    return primaryAdapter.listModels();
  }
}
