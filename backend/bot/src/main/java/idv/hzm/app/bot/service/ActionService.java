package idv.hzm.app.bot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.action.Action;
import idv.hzm.app.bot.consumer.BotConsumer;
import idv.hzm.app.bot.dto.CommonActionDto;
import idv.hzm.app.bot.utils.Json2Util;

@Service
public class ActionService {
	
	private static final Logger logger = LoggerFactory.getLogger(ActionService.class);
	private final Map<String, Action> actionMap = new HashMap<>();

	public ActionService(List<Action> actions) {
		for (Action action : actions) {
			this.actionMap.put(action.getType(), action);
		}
	}

	public String handle(String content) {
		logger.debug("Json2Util.fromJson {} befor",content);
		CommonActionDto commonActionDto = Json2Util.fromJson(content,CommonActionDto.class);
		logger.debug("Json2Util.fromJson {} after",content);
		String type = commonActionDto.getType();
		Action action = this.actionMap.get(commonActionDto.getType());
		if (action == null)
			throw new IllegalArgumentException("未知的 action: " + type);
		return action.execute(commonActionDto.getData());
	}

}
