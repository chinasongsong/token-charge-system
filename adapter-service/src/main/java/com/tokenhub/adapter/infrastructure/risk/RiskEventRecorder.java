package com.tokenhub.adapter.infrastructure.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tokenhub.adapter.infrastructure.persistence.RiskEventMapper;
import com.tokenhub.adapter.infrastructure.persistence.RiskEventPo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RiskEventRecorder {

  private static final Logger log = LoggerFactory.getLogger(RiskEventRecorder.class);

  private final RiskEventMapper riskEventMapper;
  private final ObjectMapper objectMapper;

  public RiskEventRecorder(RiskEventMapper riskEventMapper, ObjectMapper objectMapper) {
    this.riskEventMapper = riskEventMapper;
    this.objectMapper = objectMapper;
  }

  public void recordProviderFailover(String fromProvider, String toProvider, String detail) {
    try {
      RiskEventPo row = new RiskEventPo();
      row.setEventType("provider_failover");
      row.setSeverity("WARN");
      ObjectNode ctx = objectMapper.createObjectNode();
      ctx.put("from", fromProvider);
      ctx.put("to", toProvider);
      ctx.put("detail", detail == null ? "" : detail);
      row.setContextJson(objectMapper.writeValueAsString(ctx));
      riskEventMapper.insert(row);
    } catch (Exception ex) {
      log.warn("risk_events insert failed: {}", ex.toString());
    }
  }
}
