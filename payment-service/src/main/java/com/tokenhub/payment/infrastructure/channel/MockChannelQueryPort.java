package com.tokenhub.payment.infrastructure.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.payment.application.ChannelQueryPort;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderMapper;
import com.tokenhub.payment.infrastructure.persistence.PaymentOrderPo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * O-8 默认渠道查单实现（Mock）：
 *
 * <ul>
 *   <li>禁用时（默认）返回 UNKNOWN，禁止任何自动入账；
 *   <li>启用时（{@code tokenhub.payment.channel-query.mock-returns-paid=true}，仅开发/演示），
 *       对本地存在 INIT 订单返回 PAID（与 amount 一致），供集成测试串联 retry-credit；
 *   <li>真实通道请提供独立 Bean 覆盖（{@link ConditionalOnMissingBean}）。
 * </ul>
 */
@Component
@ConditionalOnMissingBean(ChannelQueryPort.class)
public class MockChannelQueryPort implements ChannelQueryPort {

  private final PaymentOrderMapper paymentOrderMapper;

  @Value("${tokenhub.payment.channel-query.mock-returns-paid:false}")
  private boolean mockReturnsPaid;

  public MockChannelQueryPort(PaymentOrderMapper paymentOrderMapper) {
    this.paymentOrderMapper = paymentOrderMapper;
  }

  @Override
  public QueryResult query(String channel, String orderNo) {
    if (!mockReturnsPaid) {
      return QueryResult.unknown();
    }
    if (orderNo == null || orderNo.isBlank()) {
      return QueryResult.unknown();
    }
    PaymentOrderPo row = paymentOrderMapper.selectOne(
        new LambdaQueryWrapper<PaymentOrderPo>().eq(PaymentOrderPo::getOrderNo, orderNo)
    );
    if (row == null) {
      return QueryResult.unknown();
    }
    if ("PAID".equalsIgnoreCase(row.getStatus())) {
      return new QueryResult(ChannelStatus.PAID, row.getAmount(), orderNo, "mock-paid");
    }
    return new QueryResult(ChannelStatus.PAID, row.getAmount(), orderNo, "mock-paid");
  }
}
