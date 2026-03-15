package idv.hzm.app.bot.exception;

import idv.hzm.app.bot.dto.IErrorCode;

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
