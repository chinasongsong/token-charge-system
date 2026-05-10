package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.billing.infrastructure.persistence.RequestOrderMapper;
import com.tokenhub.billing.infrastructure.persistence.RequestOrderPo;
import com.tokenhub.billing.infrastructure.persistence.UsageLedgerMapper;
import com.tokenhub.billing.infrastructure.persistence.UsageLedgerPo;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BillingQueryApplicationService {

  private final RequestOrderMapper requestOrderMapper;
  private final UsageLedgerMapper usageLedgerMapper;
  private final AccountBalanceApplicationService accountBalanceApplicationService;

  public BillingQueryApplicationService(
      RequestOrderMapper requestOrderMapper,
      UsageLedgerMapper usageLedgerMapper,
      AccountBalanceApplicationService accountBalanceApplicationService
  ) {
    this.requestOrderMapper = requestOrderMapper;
    this.usageLedgerMapper = usageLedgerMapper;
    this.accountBalanceApplicationService = accountBalanceApplicationService;
  }

  public record DashboardSummary(long balance, String currency, long billingOrderCount) {}

  public DashboardSummary dashboardSummary(long userId) {
    long balance = accountBalanceApplicationService.getBalance(userId);
    long count = requestOrderMapper.selectCount(
        new LambdaQueryWrapper<RequestOrderPo>().eq(RequestOrderPo::getUserId, userId)
    );
    return new DashboardSummary(balance, "TOKEN", count);
  }

  public List<RequestOrderPo> listOrders(long userId, int limit) {
    return requestOrderMapper.selectList(
        new LambdaQueryWrapper<RequestOrderPo>()
            .eq(RequestOrderPo::getUserId, userId)
            .orderByDesc(RequestOrderPo::getCreatedAt)
            .last("LIMIT " + Math.min(Math.max(limit, 1), 200))
    );
  }

  public List<UsageLedgerPo> listUsage(long userId, int limit) {
    return usageLedgerMapper.selectList(
        new LambdaQueryWrapper<UsageLedgerPo>()
            .eq(UsageLedgerPo::getUserId, userId)
            .orderByDesc(UsageLedgerPo::getRecordedAt)
            .last("LIMIT " + Math.min(Math.max(limit, 1), 200))
    );
  }
}
