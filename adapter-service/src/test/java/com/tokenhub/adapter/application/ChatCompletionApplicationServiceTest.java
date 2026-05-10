package com.tokenhub.adapter.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenhub.adapter.domain.provider.ProviderAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatCompletionApplicationServiceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Mock
  private ProviderAdapter adapter;

  private ChatCompletionApplicationService service;

  @BeforeEach
  void setUp() {
    service = new ChatCompletionApplicationService(adapter, "deepseek-v4-flash");
  }

  @Test
  void fillsMissingModelWithDefault() throws Exception {
    JsonNode body = MAPPER.readTree("{}");
    when(adapter.chat(any())).thenAnswer(inv -> inv.getArgument(0));

    JsonNode result = service.chat(body);

    ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
    verify(adapter).chat(captor.capture());
    assertThat(captor.getValue().get("model").asText()).isEqualTo("deepseek-v4-flash");
    assertThat(result.get("model").asText()).isEqualTo("deepseek-v4-flash");
  }

  @Test
  void preservesExplicitModel() throws Exception {
    ObjectNode body = MAPPER.createObjectNode();
    body.put("model", "custom-model");
    when(adapter.chat(any())).thenAnswer(inv -> inv.getArgument(0));

    JsonNode result = service.chat(body);

    assertThat(result.get("model").asText()).isEqualTo("custom-model");
  }

  @Test
  void delegatesListModels() throws Exception {
    JsonNode models = MAPPER.readTree("{\"object\":\"list\",\"data\":[]}");
    when(adapter.listModels()).thenReturn(models);

    assertThat(service.models()).isSameAs(models);
    verify(adapter).listModels();
  }
}
