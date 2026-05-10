package com.tokenhub.billing.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("model_prices")
public class ModelPricePo {

  @TableId(type = IdType.AUTO)
  private Long id;
  @TableField("provider_id")
  private Long providerId;
  private String model;
  @TableField("pricing_unit")
  private String pricingUnit;
  @TableField("input_micro")
  private Long inputMicro;
  @TableField("output_micro")
  private Long outputMicro;
  @TableField("effective_from")
  private LocalDateTime effectiveFrom;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProviderId() {
    return providerId;
  }

  public void setProviderId(Long providerId) {
    this.providerId = providerId;
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public String getPricingUnit() {
    return pricingUnit;
  }

  public void setPricingUnit(String pricingUnit) {
    this.pricingUnit = pricingUnit;
  }

  public Long getInputMicro() {
    return inputMicro;
  }

  public void setInputMicro(Long inputMicro) {
    this.inputMicro = inputMicro;
  }

  public Long getOutputMicro() {
    return outputMicro;
  }

  public void setOutputMicro(Long outputMicro) {
    this.outputMicro = outputMicro;
  }

  public LocalDateTime getEffectiveFrom() {
    return effectiveFrom;
  }

  public void setEffectiveFrom(LocalDateTime effectiveFrom) {
    this.effectiveFrom = effectiveFrom;
  }
}
