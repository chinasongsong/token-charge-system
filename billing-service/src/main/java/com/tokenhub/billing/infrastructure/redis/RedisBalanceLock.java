package com.tokenhub.billing.infrastructure.redis;

import com.tokenhub.billing.application.BalanceLock;
import com.tokenhub.common.core.error.BusinessException;
import com.tokenhub.common.core.error.ErrorCode;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * O-1 实现：账户维度短锁。优先 Redis {@code SET NX}（短 TTL，防进程异常死锁），
 * 失败或无 Redis 时回落到 JVM 进程内同步块（单实例仍可保证串行）。
 *
 * <p>等待策略：自旋尝试若干次，每次失败 sleep 一小段；超时未拿到锁，抛 {@code CONFLICT}
 * 业务异常并由调用方决定是否对客户端 retry。
 */
@Component
public class RedisBalanceLock implements BalanceLock {

  private static final Logger log = LoggerFactory.getLogger(RedisBalanceLock.class);
  private static final ConcurrentHashMap<Long, Object> JVM_LOCKS = new ConcurrentHashMap<>();

  private final ObjectProvider<StringRedisTemplate> redisProvider;

  @Value("${tokenhub.billing.balance-lock.ttl-ms:5000}")
  private long ttlMs;

  @Value("${tokenhub.billing.balance-lock.max-wait-ms:1500}")
  private long maxWaitMs;

  @Value("${tokenhub.billing.balance-lock.spin-sleep-ms:30}")
  private long spinSleepMs;

  public RedisBalanceLock(ObjectProvider<StringRedisTemplate> redisProvider) {
    this.redisProvider = redisProvider;
  }

  @Override
  public void runForUser(long userId, Runnable action) {
    StringRedisTemplate redis = redisProvider.getIfAvailable();
    String key = "lock:billing:balance:u:" + userId;
    if (redis != null) {
      String token = Long.toHexString(System.nanoTime()) + ":" + Thread.currentThread().getId();
      long deadline = System.currentTimeMillis() + Math.max(0, maxWaitMs);
      while (true) {
        Boolean ok;
        try {
          ok = redis.opsForValue().setIfAbsent(key, token, Duration.ofMillis(Math.max(100, ttlMs)));
        } catch (DataAccessException ex) {
          log.warn("RedisBalanceLock fallback to JVM, redis unavailable: {}", ex.getMessage());
          runWithJvmLock(userId, action);
          return;
        }
        if (Boolean.TRUE.equals(ok)) {
          try {
            action.run();
            return;
          } finally {
            releaseSafely(redis, key, token);
          }
        }
        if (System.currentTimeMillis() >= deadline) {
          throw new BusinessException(ErrorCode.CONFLICT, "扣费冲突，请稍后重试");
        }
        sleepBriefly(spinSleepMs);
      }
    }
    runWithJvmLock(userId, action);
  }

  private static void runWithJvmLock(long userId, Runnable action) {
    Object monitor = JVM_LOCKS.computeIfAbsent(userId, k -> new Object());
    synchronized (monitor) {
      try {
        action.run();
      } finally {
        JVM_LOCKS.remove(userId, monitor);
      }
    }
  }

  private static void releaseSafely(StringRedisTemplate redis, String key, String token) {
    try {
      String current = redis.opsForValue().get(key);
      if (token.equals(current)) {
        redis.delete(key);
      }
    } catch (DataAccessException ex) {
      log.warn("RedisBalanceLock release skipped: {}", ex.getMessage());
    }
  }

  private static void sleepBriefly(long ms) {
    try {
      Thread.sleep(Math.max(1, ms));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
