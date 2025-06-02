package idv.hzm.app.common.api;

/**
 * 常用API返回對象接口.
 */
public interface IErrorCode {

  /**
   * 返回碼
   */
  long getCode();

  /**
   * 返回信息
   */
  String getMessage();
}
