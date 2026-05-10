package com.tokenhub.payment.infrastructure.redis;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 回调短锁：优先 Redis SET NX，单机开发无 Redis 时退回 JVM 同步块；与 billing 入账幂等配合。
 */
@Component
public class PaymentCallbackOrderLock {

  private static final ConcurrentHashMap<String, Object> JVM_LOCKS = new ConcurrentHashMap<>();

  private final ObjectProvider<StringRedisTemplate> redisProvider;

  public PaymentCallbackOrderLock(ObjectProvider<StringRedisTemplate> redisProvider) {
    this.redisProvider = redisProvider;
  }

  public void run(String orderNo, Runnable action) {
    StringRedisTemplate redis = redisProvider.getIfAvailable();
    String key = "lock:payment:callback:" + orderNo;
    if (redis != null) {
      if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(50)))) {
        try {
          action.run();
        } finally {
          redis.delete(key);
        }
        return;
      }
      sleepBriefly();
      action.run();
      return;
    }
    Object o = JVM_LOCKS.computeIfAbsent(orderNo, k -> new Object());
    synchronized (o) {
      try {
        action.run();
      } finally {
        JVM_LOCKS.remove(orderNo, o);
      }
    }
  }

  private static void sleepBriefly() {
    try {
      Thread.sleep(40);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
