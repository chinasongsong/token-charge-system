package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.billing.infrastructure.persistence.AccountBalanceMapper;
import com.tokenhub.billing.infrastructure.persistence.AccountBalancePo;
import com.tokenhub.billing.infrastructure.persistence.BalanceTopupReceiptMapper;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountBalanceApplicationService {

  private final AccountBalanceMapper accountBalanceMapper;
  private final BalanceTopupReceiptMapper balanceTopupReceiptMapper;

  public AccountBalanceApplicationService(
      AccountBalanceMapper accountBalanceMapper,
      BalanceTopupReceiptMapper balanceTopupReceiptMapper
  ) {
    this.accountBalanceMapper = accountBalanceMapper;
    this.balanceTopupReceiptMapper = balanceTopupReceiptMapper;
  }

  public AccountBalancePo getOrCreate(long userId) {
    AccountBalancePo row = accountBalanceMapper.selectOne(
        new LambdaQueryWrapper<AccountBalancePo>().eq(AccountBalancePo::getUserId, userId)
    );
    if (row != null) {
      return row;
    }
    try {
      AccountBalancePo insert = new AccountBalancePo();
      insert.setUserId(userId);
      insert.setBalance(0L);
      insert.setCurrency("TOKEN");
      accountBalanceMapper.insert(insert);
    } catch (DuplicateKeyException ignored) {
      // concurrent create
    }
    return accountBalanceMapper.selectOne(
        new LambdaQueryWrapper<AccountBalancePo>().eq(AccountBalancePo::getUserId, userId)
    );
  }

  public long getBalance(long userId) {
    return getOrCreate(userId).getBalance();
  }

  @Transactional
  public void debit(long userId, long amount) {
    if (amount <= 0) {
      return;
    }
    for (int attempt = 0; attempt < 8; attempt++) {
      AccountBalancePo row = getOrCreate(userId);
      if (row.getBalance() < amount) {
        throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, "余额不足");
      }
      row.setBalance(row.getBalance() - amount);
      int rows = accountBalanceMapper.updateById(row);
      if (rows == 1) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.CONFLICT, "扣款冲突，请重试");
  }

  @Transactional
  public void credit(long userId, long amount) {
    if (amount <= 0) {
      return;
    }
    for (int attempt = 0; attempt < 8; attempt++) {
      AccountBalancePo row = getOrCreate(userId);
      row.setBalance(row.getBalance() + amount);
      int rows = accountBalanceMapper.updateById(row);
      if (rows == 1) {
        return;
      }
    }
    throw new BusinessException(ErrorCode.CONFLICT, "入账冲突，请重试");
  }

  /**
   * 支付等外部来源入账；与 {@code sourceRef} 幂等（同 ref 仅入账一次）。
   */
  @Transactional
  public void creditIdempotent(long userId, long amount, String sourceRef) {
    if (amount <= 0 || sourceRef == null || sourceRef.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "入账参数不合法");
    }
    int n = balanceTopupReceiptMapper.insertIgnore(sourceRef, userId, amount);
    if (n == 0) {
      return;
    }
    credit(userId, amount);
  }

  public void assertPositiveBalance(long userId) {
    long b = getBalance(userId);
    if (b <= 0) {
      throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, "余额不足，请先充值");
    }
  }
}
