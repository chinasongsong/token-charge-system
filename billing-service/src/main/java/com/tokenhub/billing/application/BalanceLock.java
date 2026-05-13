package com.tokenhub.billing.application;

/**
 * 账户维度互斥锁（应用层端口）。
 *
 * <p>典型实现：Redis {@code SET NX} + 短 TTL，无 Redis 时回落到 JVM 内 {@code synchronized}。
 *
 * <p>语义：单线程化某用户的扣费/结算关键段，降低乐观锁重试与「读取-计算-写回」窗口竞争；
 * 与现有 {@code account_balance.version} 乐观锁、{@code request_orders.idempotency_key} 幂等
 * 三者形成 <b>串行 + 一致 + 防重</b> 的组合。
 */
public interface BalanceLock {

  /**
   * 在「用户维度」获取锁后执行 action。锁内异常向上抛出，锁会被释放。
   *
   * @param userId 用户主键
   * @param action 需要在锁内执行的业务动作
   */
  void runForUser(long userId, Runnable action);
}
