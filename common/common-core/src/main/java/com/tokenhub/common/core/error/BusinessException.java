package com.tokenhub.common.core.error;

/**
 * Domain/application level failure mapped to unified {@link com.tokenhub.common.core.api.ApiResponse}.
 */
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final String detailMessage;

  public BusinessException(ErrorCode errorCode, String detailMessage, Throwable cause) {
    super(detailMessage != null ? detailMessage : errorCode.getDefaultMessage(), cause);
    this.errorCode = errorCode;
    this.detailMessage = detailMessage != null ? detailMessage : errorCode.getDefaultMessage();
  }

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, null, null);
  }

  public BusinessException(ErrorCode errorCode, String detailMessage) {
    this(errorCode, detailMessage, null);
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public String getDetailMessage() {
    return detailMessage;
  }
}
