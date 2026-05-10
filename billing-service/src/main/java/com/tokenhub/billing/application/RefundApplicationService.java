package com.tokenhub.billing.application;

import com.tokenhub.billing.infrastructure.persistence.RefundRequestMapper;
import com.tokenhub.billing.infrastructure.persistence.RefundRequestPo;
import org.springframework.stereotype.Service;

@Service
public class RefundApplicationService {

  private final RefundRequestMapper refundRequestMapper;

  public RefundApplicationService(RefundRequestMapper refundRequestMapper) {
    this.refundRequestMapper = refundRequestMapper;
  }

  public RefundRequestPo apply(long userId, String orderNo, long amount, String reason) {
    RefundRequestPo row = new RefundRequestPo();
    row.setUserId(userId);
    row.setOrderNo(orderNo);
    row.setAmount(amount);
    row.setReason(reason);
    row.setStatus("PENDING");
    refundRequestMapper.insert(row);
    return row;
  }
}
