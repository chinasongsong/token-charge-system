package com.tokenhub.adapter.domain.provider;

import com.fasterxml.jackson.databind.JsonNode;

public interface ProviderAdapter {

  String providerCode();

  JsonNode chat(JsonNode openAiRequestBody);

  JsonNode listModels();

  default JsonNode embeddings(JsonNode openAiRequestBody) {
    throw new UnsupportedOperationException("embeddings");
  }
}
