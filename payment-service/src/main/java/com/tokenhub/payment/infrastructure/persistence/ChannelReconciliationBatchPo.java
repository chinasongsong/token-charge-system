package com.tokenhub.payment.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("channel_reconciliation_batches")
public class ChannelReconciliationBatchPo {

  @TableId(type = IdType.AUTO)
  private Long id;

  private String channel;

  @TableField("bill_date")
  private LocalDate billDate;

  @TableField("source_name")
  private String sourceName;

  @TableField("total_lines")
  private Integer totalLines;

  @TableField("matched_lines")
  private Integer matchedLines;

  @TableField("mismatched_lines")
  private Integer mismatchedLines;

  private String status;

  @TableField("created_at")
  private LocalDateTime createdAt;

  @TableField("updated_at")
  private LocalDateTime updatedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public LocalDate getBillDate() {
    return billDate;
  }

  public void setBillDate(LocalDate billDate) {
    this.billDate = billDate;
  }

  public String getSourceName() {
    return sourceName;
  }

  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  public Integer getTotalLines() {
    return totalLines;
  }

  public void setTotalLines(Integer totalLines) {
    this.totalLines = totalLines;
  }

  public Integer getMatchedLines() {
    return matchedLines;
  }

  public void setMatchedLines(Integer matchedLines) {
    this.matchedLines = matchedLines;
  }

  public Integer getMismatchedLines() {
    return mismatchedLines;
  }

  public void setMismatchedLines(Integer mismatchedLines) {
    this.mismatchedLines = mismatchedLines;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }
}
