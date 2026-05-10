package com.tokenhub.adapter.infrastructure.provider;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.adapter.infrastructure.persistence.ModelProviderMapper;
import com.tokenhub.adapter.infrastructure.persistence.ModelProviderPo;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ModelProviderRegistry {

  private final ModelProviderMapper mapper;

  public ModelProviderRegistry(ModelProviderMapper mapper) {
    this.mapper = mapper;
  }

  public Optional<ModelProviderPo> findEnabled(String code) {
    return Optional.ofNullable(
        mapper.selectOne(
            new LambdaQueryWrapper<ModelProviderPo>()
                .eq(ModelProviderPo::getCode, code)
                .eq(ModelProviderPo::getEnabled, true)
        )
    );
  }
}
