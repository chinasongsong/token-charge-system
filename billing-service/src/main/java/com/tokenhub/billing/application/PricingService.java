package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.billing.infrastructure.persistence.ModelPriceMapper;
import com.tokenhub.billing.infrastructure.persistence.ModelPricePo;
import com.tokenhub.billing.infrastructure.persistence.ModelProviderMapper;
import com.tokenhub.billing.infrastructure.persistence.ModelProviderPo;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

  private final ModelProviderMapper modelProviderMapper;
  private final ModelPriceMapper modelPriceMapper;

  public PricingService(ModelProviderMapper modelProviderMapper, ModelPriceMapper modelPriceMapper) {
    this.modelProviderMapper = modelProviderMapper;
    this.modelPriceMapper = modelPriceMapper;
  }

  /**
   * @return 扣费数量（与 {@code account_balance.balance} 同量纲的 milli 风格累加单位）
   */
  public long computeChargeMicro(String providerCode, String modelName, long inputTokens, long outputTokens) {
    ModelProviderPo provider = modelProviderMapper.selectOne(
        new LambdaQueryWrapper<ModelProviderPo>()
            .eq(ModelProviderPo::getCode, providerCode)
            .last("LIMIT 1")
    );
    if (provider == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未知供应商: " + providerCode);
    }
    ModelPricePo price = modelPriceMapper.selectOne(
        new LambdaQueryWrapper<ModelPricePo>()
            .eq(ModelPricePo::getProviderId, provider.getId())
            .eq(ModelPricePo::getModel, modelName)
            .eq(ModelPricePo::getPricingUnit, "TOKEN")
            .last("LIMIT 1")
    );
    if (price == null || price.getInputMicro() == null || price.getOutputMicro() == null) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "未配置模型计价: " + providerCode + "/" + modelName);
    }
    long inChunks = (inputTokens + 999) / 1000;
    long outChunks = (outputTokens + 999) / 1000;
    return inChunks * price.getInputMicro() + outChunks * price.getOutputMicro();
  }
}
