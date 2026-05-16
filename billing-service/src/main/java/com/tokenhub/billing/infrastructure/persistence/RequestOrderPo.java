package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("request_orders")
public class RequestOrderPo {

  @TableId(type = IdType.AUTO)
  private Long id;
  @TableField("trace_id")
  private String traceId;
  @TableField("api_key_id")
  private Long apiKeyId;
  @TableField("user_id")
  private Long userId;
  @TableField("provider_code")
  private String providerCode;
  @TableField("model_name")
  private String modelName;
  @TableField("billing_status")
  private String billingStatus;
  @TableField("input_tokens")
  private Long inputTokens;
  @TableField("output_tokens")
  private Long outputTokens;
  private Long amount;
  @TableField("idempotency_key")
  private String idempotencyKey;
  @TableField("idempotency_source")
  private String idempotencySource;
  @TableField("idempotency_expires_at")
  private LocalDateTime idempotencyExpiresAt;
  @TableField("created_at")
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public Long getApiKeyId() {
    return apiKeyId;
  }

  public void setApiKeyId(Long apiKeyId) {
    this.apiKeyId = apiKeyId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getProviderCode() {
    return providerCode;
  }

  public void setProviderCode(String providerCode) {
    this.providerCode = providerCode;
  }

  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  public String getBillingStatus() {
    return billingStatus;
  }

  public void setBillingStatus(String billingStatus) {
    this.billingStatus = billingStatus;
  }

  public Long getInputTokens() {
    return inputTokens;
  }

  public void setInputTokens(Long inputTokens) {
    this.inputTokens = inputTokens;
  }

  public Long getOutputTokens() {
    return outputTokens;
  }

  public void setOutputTokens(Long outputTokens) {
    this.outputTokens = outputTokens;
  }

  public Long getAmount() {
    return amount;
  }

  public void setAmount(Long amount) {
    this.amount = amount;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getIdempotencySource() {
    return idempotencySource;
  }

  public void setIdempotencySource(String idempotencySource) {
    this.idempotencySource = idempotencySource;
  }

  public LocalDateTime getIdempotencyExpiresAt() {
    return idempotencyExpiresAt;
  }

  public void setIdempotencyExpiresAt(LocalDateTime idempotencyExpiresAt) {
    this.idempotencyExpiresAt = idempotencyExpiresAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
