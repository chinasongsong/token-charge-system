package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("invoices")
public class InvoicePo {

  @TableId(type = IdType.AUTO)
  private Long id;
  @TableField("user_id")
  private Long userId;
  @TableField("order_ref")
  private String orderRef;
  private Long amount;
  private String currency;
  @TableField("pdf_number")
  private String pdfNumber;
  private String status;
  @TableField("created_at")
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public String getOrderRef() { return orderRef; }
  public void setOrderRef(String orderRef) { this.orderRef = orderRef; }
  public Long getAmount() { return amount; }
  public void setAmount(Long amount) { this.amount = amount; }
  public String getCurrency() { return currency; }
  public void setCurrency(String currency) { this.currency = currency; }
  public String getPdfNumber() { return pdfNumber; }
  public void setPdfNumber(String pdfNumber) { this.pdfNumber = pdfNumber; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
