package idv.hzm.app.common.api;

/**
 * 常用API返回對象.
 */
public enum ResultCode implements IErrorCode {

  SUCCESS(200, "操作成功"), 
  FAILED(500, "操作失敗"), 
  VALIDATE_FAILED(404, "參數檢驗失敗"), 
  UNAUTHORIZED(401,"暫未登錄或token已經過期"), 
  FORBIDDEN(403, "沒有相關權限");

  private long code;
  private String message;

  private ResultCode(long code, String message) {
    this.code = code;
    this.message = message;
  }

  public long getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
