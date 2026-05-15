package com.tokenhub.common.mybatis.routing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * O-9：AOP 拦截 {@link ReadOnly} 注解，方法入口压 READER、出口弹栈。
 *
 * <p>优先级高于 Spring 事务（{@code @Transactional} 默认 LOWEST_PRECEDENCE - 1），
 * 保证 READER 路由在事务获取连接 <b>前</b> 设置。
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class ReadOnlyRoutingAspect {

  @Around("@annotation(com.tokenhub.common.mybatis.routing.ReadOnly)")
  public Object route(ProceedingJoinPoint pjp) throws Throwable {
    RoutingContext.push(RoutingContext.Route.READER);
    try {
      return pjp.proceed();
    } finally {
      RoutingContext.pop();
    }
  }
}
