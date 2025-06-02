package idv.hzm.app.common.exception;

import idv.hzm.app.common.api.IErrorCode;

/**
 * Custom API exception.
 */
public class ApiException extends RuntimeException {
  
  /**
   * 
   */
  private static final long serialVersionUID = 4708306518230049485L;
  
  private IErrorCode errorCode;

  public ApiException(IErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

  public ApiException(String message) {
    super(message);
  }

  public ApiException(Throwable cause) {
    super(cause);
  }

  public ApiException(String message, Throwable cause) {
    super(message, cause);
  }

  public IErrorCode getErrorCode() {
    return errorCode;
  }
}
