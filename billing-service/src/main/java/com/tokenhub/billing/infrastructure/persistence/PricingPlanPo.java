package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("pricing_plans")
public class PricingPlanPo {

  @TableId(type = IdType.AUTO)
  private Long id;
  private String code;
  private String name;
  private Long price;
  private String cycle;
  private String status;
  private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Long getPrice() { return price; }
  public void setPrice(Long price) { this.price = price; }
  public String getCycle() { return cycle; }
  public void setCycle(String cycle) { this.cycle = cycle; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
