package idv.hzm.app.common.exception;

import idv.hzm.app.common.api.IErrorCode;

/**
 * Assertion processing class for throwing various API exceptions.
 */
public class Asserts {
  
  public static void fail(String message) {
    throw new ApiException(message);
  }

  public static void fail(IErrorCode errorCode) {
    throw new ApiException(errorCode);
  }
}
