package idv.hzm.app.bot.action;

import java.lang.reflect.ParameterizedType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import idv.hzm.app.bot.consumer.BotConsumer;
import idv.hzm.app.common.util.Json2Util;

public abstract class Action<T> {
	private static final Logger logger = LoggerFactory.getLogger(BotConsumer.class);
	
	private final Class<T> type;

	@SuppressWarnings("unchecked")
	public Action() {
		ParameterizedType superClass = (ParameterizedType) getClass().getGenericSuperclass();// 利用反射抓取子類別實際的泛型類型
		this.type = (Class<T>) superClass.getActualTypeArguments()[0];
	}

	public String execute(String data) {
		logger.debug("execute Json2Util.fromJson {} befor",data);
		T dataObj = Json2Util.fromJson(data, this.type);// 將 Json 轉成對應的 T
		logger.debug("execute Json2Util.fromJson {} befor",data);
		return Json2Util.toJson(handle(dataObj))  ;
	}

	public abstract String getType();

	protected abstract Object handle(T data);
}
