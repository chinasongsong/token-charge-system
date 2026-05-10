package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("usage_ledger")
public class UsageLedgerPo {

  @TableId(type = IdType.AUTO)
  private Long id;
  @TableField("user_id")
  private Long userId;
  @TableField("request_order_id")
  private Long requestOrderId;
  @TableField("entry_type")
  private String entryType;
  private Long quantity;
  @TableField("idempotency_key")
  private String idempotencyKey;
  @TableField("detail_json")
  private String detailJson;
  @TableField("recorded_at")
  private LocalDateTime recordedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getRequestOrderId() {
    return requestOrderId;
  }

  public void setRequestOrderId(Long requestOrderId) {
    this.requestOrderId = requestOrderId;
  }

  public String getEntryType() {
    return entryType;
  }

  public void setEntryType(String entryType) {
    this.entryType = entryType;
  }

  public Long getQuantity() {
    return quantity;
  }

  public void setQuantity(Long quantity) {
    this.quantity = quantity;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getDetailJson() {
    return detailJson;
  }

  public void setDetailJson(String detailJson) {
    this.detailJson = detailJson;
  }

  public LocalDateTime getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(LocalDateTime recordedAt) {
    this.recordedAt = recordedAt;
  }
}
