package idv.hzm.app.bot.dto;

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
