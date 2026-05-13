package com.tokenhub.common.mybatis.routing;

/**
 * O-9：当前线程的数据源路由目标。AOP 在 {@link ReadOnly} 方法入口压栈 READER，退出弹栈。
 *
 * <p>线程局部栈：支持 ReadOnly 方法内调用另一个 ReadOnly 方法（嵌套）。
 */
public final class RoutingContext {

  public enum Route {
    WRITER,
    READER
  }

  private static final ThreadLocal<java.util.Deque<Route>> STACK =
      ThreadLocal.withInitial(java.util.ArrayDeque::new);

  private RoutingContext() {}

  public static void push(Route route) {
    STACK.get().push(route);
  }

  public static void pop() {
    java.util.Deque<Route> stack = STACK.get();
    if (!stack.isEmpty()) {
      stack.pop();
    }
    if (stack.isEmpty()) {
      STACK.remove();
    }
  }

  public static Route currentOrDefault() {
    java.util.Deque<Route> stack = STACK.get();
    return stack.isEmpty() ? Route.WRITER : stack.peek();
  }
}
