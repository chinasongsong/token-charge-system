package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.billing.infrastructure.persistence.PricingPlanMapper;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import com.tokenhub.billing.infrastructure.persistence.PricingPlanPo;
import com.tokenhub.billing.infrastructure.persistence.UserSubscriptionMapper;
import com.tokenhub.billing.infrastructure.persistence.UserSubscriptionPo;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CatalogAndSubscriptionApplicationService {

  private final PricingPlanMapper pricingPlanMapper;
  private final UserSubscriptionMapper userSubscriptionMapper;

  public CatalogAndSubscriptionApplicationService(
      PricingPlanMapper pricingPlanMapper,
      UserSubscriptionMapper userSubscriptionMapper
  ) {
    this.pricingPlanMapper = pricingPlanMapper;
    this.userSubscriptionMapper = userSubscriptionMapper;
  }

  public List<PricingPlanPo> listActivePlans() {
    return pricingPlanMapper.selectList(
        new LambdaQueryWrapper<PricingPlanPo>().eq(PricingPlanPo::getStatus, "ACTIVE")
    );
  }

  public UserSubscriptionPo subscribe(long userId, String planCode) {
    PricingPlanPo plan = pricingPlanMapper.selectOne(
        new LambdaQueryWrapper<PricingPlanPo>()
            .eq(PricingPlanPo::getCode, planCode)
            .eq(PricingPlanPo::getStatus, "ACTIVE")
    );
    if (plan == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "套餐不存在或未开放");
    }
    UserSubscriptionPo row = new UserSubscriptionPo();
    row.setUserId(userId);
    row.setPlanId(plan.getId());
    row.setStatus("ACTIVE");
    row.setStartedAt(LocalDateTime.now());
    row.setEndsAt(null);
    userSubscriptionMapper.insert(row);
    return row;
  }
}
