package idv.hzm.app.bot.consumer;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.config.WebSocketConfig;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.utils.Json2Util;

@Component
public class ReplyUserConsumer implements MessageListener {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Override
	public void onMessage(Message message) {
		String payload = new String(message.getBody(), StandardCharsets.UTF_8);
		EventDto chatMessage = Json2Util.fromJson(payload, EventDto.class);
		this.redisTemplate.convertAndSend(WebSocketConfig.WEBSOCKET_CHANNEL_NAME, chatMessage);
	}
}