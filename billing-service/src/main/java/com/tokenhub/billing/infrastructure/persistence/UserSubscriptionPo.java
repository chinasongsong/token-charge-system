package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("user_subscriptions")
public class UserSubscriptionPo {

  @TableId(type = IdType.AUTO)
  private Long id;
  @TableField("user_id")
  private Long userId;
  @TableField("plan_id")
  private Long planId;
  private String status;
  @TableField("started_at")
  private LocalDateTime startedAt;
  @TableField("ends_at")
  private LocalDateTime endsAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getUserId() { return userId; }
  public void setUserId(Long userId) { this.userId = userId; }
  public Long getPlanId() { return planId; }
  public void setPlanId(Long planId) { this.planId = planId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public LocalDateTime getStartedAt() { return startedAt; }
  public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
  public LocalDateTime getEndsAt() { return endsAt; }
  public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
}
