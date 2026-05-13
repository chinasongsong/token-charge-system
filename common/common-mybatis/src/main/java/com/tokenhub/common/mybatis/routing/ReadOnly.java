package com.tokenhub.common.mybatis.routing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * O-9：方法级只读路由标记。被该注解标注的方法及其下游 MyBatis 查询，
 * 优先路由到只读副本（若 {@code DataSourceRoutingConfiguration} 配置启用且存在 reader）。
 *
 * <p>语义：标记只是「提示路由」；如未启用动态数据源或未配置 reader，调用仍走主库。
 * <b>禁止</b>在写事务或资金扣费链路使用本注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {}
