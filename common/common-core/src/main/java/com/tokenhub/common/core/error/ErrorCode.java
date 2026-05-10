package com.tokenhub.common.core.error;

/**
 * Canonical platform error codes; extend conservatively across phases.
 */
public enum ErrorCode {
  SUCCESS("0", "成功"),
  BAD_REQUEST("I400001", "请求参数不合法"),
  UNAUTHORIZED("I401001", "未登录或令牌无效"),
  FORBIDDEN("I403001", "无权限"),
  NOT_FOUND("I404001", "资源不存在"),
  CONFLICT("I409001", "资源冲突"),
  TOO_MANY_REQUESTS("I429001", "请求过于频繁"),
  INTERNAL("I500001", "服务内部异常"),
  BALANCE_INSUFFICIENT("B402001", "余额不足"),
  ;

  private final String code;
  private final String defaultMessage;

  ErrorCode(String code, String defaultMessage) {
    this.code = code;
    this.defaultMessage = defaultMessage;
  }

  public String getCode() {
    return code;
  }

  public String getDefaultMessage() {
    return defaultMessage;
  }
}
