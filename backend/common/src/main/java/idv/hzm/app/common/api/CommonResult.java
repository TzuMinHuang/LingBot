package idv.hzm.app.common.api;

/**
 * 通用返回對象
 */
public class CommonResult<T> {

  /**
   * 狀態碼
   */
  private long code;
  /**
   * 提示信息
   */
  private String message;
  /**
   * 數據封裝
   */
  private T data;

  protected CommonResult() {}

  protected CommonResult(long code, String message, T data) {
    this.code = code;
    this.message = message;
    this.data = data;
  }

  public long getCode() {
    return code;
  }

  public void setCode(long code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }

  /**
   * 成功返回結果
   *
   * @param data 獲取的數據
   */
  public static <T> CommonResult<T> success(T data) {
    return new CommonResult<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
  }

  /**
   * 成功返回結果
   *
   * @param data 獲取的數據
   * @param message 提示信息
   */
  public static <T> CommonResult<T> success(T data, String message) {
    return new CommonResult<T>(ResultCode.SUCCESS.getCode(), message, data);
  }

  /**
   * 失敗返回結果
   * 
   * @param errorCode 錯誤碼
   */
  public static <T> CommonResult<T> failed(IErrorCode errorCode) {
    return new CommonResult<T>(errorCode.getCode(), errorCode.getMessage(), null);
  }

  /**
   * 失敗返回結果
   * 
   * @param errorCode 錯誤碼
   * @param message 錯誤信息
   */
  public static <T> CommonResult<T> failed(IErrorCode errorCode, String message) {
    return new CommonResult<T>(errorCode.getCode(), message, null);
  }

  /**
   * 失敗返回結果
   * 
   * @param message 提示信息
   */
  public static <T> CommonResult<T> failed(String message) {
    return new CommonResult<T>(ResultCode.FAILED.getCode(), message, null);
  }

  /**
   * 失敗返回結果
   */
  public static <T> CommonResult<T> failed() {
    return failed(ResultCode.FAILED);
  }

  /**
   * 參數驗證失敗返回結果
   */
  public static <T> CommonResult<T> validateFailed() {
    return failed(ResultCode.VALIDATE_FAILED);
  }

  /**
   * 參數驗證失敗返回結果
   * 
   * @param message 提示信息
   */
  public static <T> CommonResult<T> validateFailed(String message) {
    return new CommonResult<T>(ResultCode.VALIDATE_FAILED.getCode(), message, null);
  }

  /**
   * 未登錄返回結果
   */
  public static <T> CommonResult<T> unauthorized(T data) {
    return new CommonResult<T>(ResultCode.UNAUTHORIZED.getCode(),
        ResultCode.UNAUTHORIZED.getMessage(), data);
  }

  /**
   * 未授權返回結果
   */
  public static <T> CommonResult<T> forbidden(T data) {
    return new CommonResult<T>(ResultCode.FORBIDDEN.getCode(), ResultCode.FORBIDDEN.getMessage(),
        data);
  }
}
