package com.tokenhub.adapter.infrastructure.routing;

import com.tokenhub.adapter.domain.routing.ProviderRoute;
import com.tokenhub.adapter.domain.routing.RoutingPolicy;
import com.tokenhub.adapter.infrastructure.zhipu.ZhipuProviderAdapter;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Weighted random first hop: e.g. deepseek=70 zhipu=30 — weights are integers, any positive sum.
 */
@Component
public class WeightedRoutingPolicy implements RoutingPolicy {

  private final ZhipuProviderAdapter zhipuProviderAdapter;
  private final int deepseekWeight;
  private final int zhipuWeight;

  public WeightedRoutingPolicy(
      ZhipuProviderAdapter zhipuProviderAdapter,
      @Value("${tokenhub.adapter.routing.weight-deepseek:70}") int deepseekWeight,
      @Value("${tokenhub.adapter.routing.weight-zhipu:30}") int zhipuWeight
  ) {
    this.zhipuProviderAdapter = zhipuProviderAdapter;
    this.deepseekWeight = Math.max(0, deepseekWeight);
    this.zhipuWeight = Math.max(0, zhipuWeight);
  }

  @Override
  public ProviderRoute chooseFirstHop() {
    if (!zhipuProviderAdapter.isConfigured() || zhipuWeight <= 0) {
      return ProviderRoute.DEEPSEEK;
    }
    if (deepseekWeight <= 0) {
      return ProviderRoute.ZHIPU;
    }
    int total = deepseekWeight + zhipuWeight;
    int r = ThreadLocalRandom.current().nextInt(total);
    return r < deepseekWeight ? ProviderRoute.DEEPSEEK : ProviderRoute.ZHIPU;
  }
}
