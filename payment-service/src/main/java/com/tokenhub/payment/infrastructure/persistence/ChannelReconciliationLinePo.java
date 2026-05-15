package com.tokenhub.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("channel_reconciliation_lines")
public class ChannelReconciliationLinePo {

  @TableId(type = IdType.AUTO)
  private Long id;

  @TableField("batch_id")
  private Long batchId;

  @TableField("channel_order_no")
  private String channelOrderNo;

  @TableField("local_order_no")
  private String localOrderNo;

  @TableField("user_id")
  private Long userId;

  @TableField("channel_amount")
  private Long channelAmount;

  @TableField("local_amount")
  private Long localAmount;

  @TableField("channel_status")
  private String channelStatus;

  @TableField("local_status")
  private String localStatus;

  @TableField("diff_kind")
  private String diffKind;

  @TableField("paid_at")
  private LocalDateTime paidAt;

  @TableField("created_at")
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBatchId() {
    return batchId;
  }

  public void setBatchId(Long batchId) {
    this.batchId = batchId;
  }

  public String getChannelOrderNo() {
    return channelOrderNo;
  }

  public void setChannelOrderNo(String channelOrderNo) {
    this.channelOrderNo = channelOrderNo;
  }

  public String getLocalOrderNo() {
    return localOrderNo;
  }

  public void setLocalOrderNo(String localOrderNo) {
    this.localOrderNo = localOrderNo;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getChannelAmount() {
    return channelAmount;
  }

  public void setChannelAmount(Long channelAmount) {
    this.channelAmount = channelAmount;
  }

  public Long getLocalAmount() {
    return localAmount;
  }

  public void setLocalAmount(Long localAmount) {
    this.localAmount = localAmount;
  }

  public String getChannelStatus() {
    return channelStatus;
  }

  public void setChannelStatus(String channelStatus) {
    this.channelStatus = channelStatus;
  }

  public String getLocalStatus() {
    return localStatus;
  }

  public void setLocalStatus(String localStatus) {
    this.localStatus = localStatus;
  }

  public String getDiffKind() {
    return diffKind;
  }

  public void setDiffKind(String diffKind) {
    this.diffKind = diffKind;
  }

  public LocalDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(LocalDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
