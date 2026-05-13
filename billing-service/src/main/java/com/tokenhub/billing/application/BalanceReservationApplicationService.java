package com.tokenhub.billing.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tokenhub.billing.infrastructure.persistence.AccountBalancePo;
import com.tokenhub.billing.infrastructure.persistence.BalanceReservationMapper;
import com.tokenhub.billing.infrastructure.persistence.BalanceReservationPo;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * O-3 预占额度与冲正服务（M1 骨架）：
 *
 * <ul>
 *   <li>{@code reserve}：校验「可用余额 = 现存余额 - 已预占有效金额 - 本次申请额」≥ 0；写入 RESERVED 行（TTL）。
 *   <li>{@code commit}：状态 RESERVED → COMMITTED；保留实际扣费金额便于审计。<b>本方法不直接扣款</b>，
 *       由调用方继续调用 {@code /settle}（settle 已是幂等）；后续 M2 可将 commit + debit 合一。
 *   <li>{@code release}：状态 RESERVED → RELEASED；幂等。
 * </ul>
 *
 * <p>幂等键：{@code trace_id}（唯一）。{@code commit/release} 对已终结状态返回当前状态而不抛错。
 */
@Service
public class BalanceReservationApplicationService {

  private final BalanceReservationMapper reservationMapper;
  private final AccountBalanceApplicationService accountBalanceApplicationService;

  @Value("${tokenhub.billing.reservation.default-ttl-seconds:120}")
  private int defaultTtlSeconds;

  public BalanceReservationApplicationService(
      BalanceReservationMapper reservationMapper,
      AccountBalanceApplicationService accountBalanceApplicationService
  ) {
    this.reservationMapper = reservationMapper;
    this.accountBalanceApplicationService = accountBalanceApplicationService;
  }

  public record ReservationView(String traceId, long userId, long amount, String status, LocalDateTime expiresAt) {}

  /**
   * 预占额度；返回包含状态的视图。重复 traceId 视为幂等：返回既有行，不再次校验余额。
   */
  @Transactional
  public ReservationView reserve(String traceId, long userId, long amount) {
    requireValid(traceId, amount);
    BalanceReservationPo existing = reservationMapper.selectOne(
        new LambdaQueryWrapper<BalanceReservationPo>().eq(BalanceReservationPo::getTraceId, traceId)
    );
    if (existing != null) {
      return toView(existing);
    }
    AccountBalancePo balance = accountBalanceApplicationService.getOrCreate(userId);
    LocalDateTime now = LocalDateTime.now();
    long activeReserved = reservationMapper.sumActiveReservedAmount(userId, now);
    long available = balance.getBalance() - activeReserved;
    if (available < amount) {
      throw new BusinessException(ErrorCode.BALANCE_INSUFFICIENT, "可用余额不足");
    }
    BalanceReservationPo po = new BalanceReservationPo();
    po.setTraceId(traceId);
    po.setUserId(userId);
    po.setAmount(amount);
    po.setStatus("RESERVED");
    po.setExpiresAt(now.plusSeconds(Math.max(10, defaultTtlSeconds)));
    try {
      reservationMapper.insert(po);
    } catch (DuplicateKeyException ex) {
      BalanceReservationPo dup = reservationMapper.selectOne(
          new LambdaQueryWrapper<BalanceReservationPo>().eq(BalanceReservationPo::getTraceId, traceId)
      );
      if (dup != null) {
        return toView(dup);
      }
      throw ex;
    }
    return toView(po);
  }

  /**
   * 冲正：状态 RESERVED → COMMITTED；committedAmount 仅作审计字段。已 COMMITTED/RELEASED 时返回当前状态。
   */
  @Transactional
  public ReservationView commit(String traceId, long committedAmount) {
    if (committedAmount < 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "commit 金额不能为负");
    }
    BalanceReservationPo row = requireRow(traceId);
    if (!"RESERVED".equals(row.getStatus())) {
      return toView(row);
    }
    row.setStatus("COMMITTED");
    row.setCommittedAmount(committedAmount);
    reservationMapper.updateById(row);
    return toView(row);
  }

  /**
   * 释放：状态 RESERVED → RELEASED；其它状态视为幂等。
   */
  @Transactional
  public ReservationView release(String traceId) {
    BalanceReservationPo row = requireRow(traceId);
    if (!"RESERVED".equals(row.getStatus())) {
      return toView(row);
    }
    row.setStatus("RELEASED");
    reservationMapper.updateById(row);
    return toView(row);
  }

  private BalanceReservationPo requireRow(String traceId) {
    if (traceId == null || traceId.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "traceId 不能为空");
    }
    BalanceReservationPo row = reservationMapper.selectOne(
        new LambdaQueryWrapper<BalanceReservationPo>().eq(BalanceReservationPo::getTraceId, traceId)
    );
    if (row == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "预占记录不存在");
    }
    return row;
  }

  private static void requireValid(String traceId, long amount) {
    if (traceId == null || traceId.isBlank()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "traceId 不能为空");
    }
    if (amount <= 0) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "amount 必须 > 0");
    }
  }

  private static ReservationView toView(BalanceReservationPo po) {
    return new ReservationView(po.getTraceId(), po.getUserId(), po.getAmount(), po.getStatus(), po.getExpiresAt());
  }
}
