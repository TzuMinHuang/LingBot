package idv.hzm.app.portal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import idv.hzm.app.common.dto.EventDto;
import idv.hzm.app.portal.config.WebSocketConfig;

@Component
public class WebSocketPublisher {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	public void publish(EventDto chatMessage) {
		this.redisTemplate.convertAndSend(WebSocketConfig.WEBSOCKET_CHANNEL_NAME, chatMessage);
	}

}
