package idv.hzm.app.bot.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.dto.EventDto;

/**
 * Redis Pub/Sub → SSE bridge 訂閱 ws-channel，收到訊息後轉發給本實例持有的 SseEmitter
 */
@Configuration
public class SseRedisSubscriber {

	private static final Logger logger = LoggerFactory.getLogger(SseRedisSubscriber.class);

	public static final String CHANNEL_NAME = "ws-channel";

	@Autowired
	private SseEmitterManager sseEmitterManager;

	@Bean
	public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory,
			MessageListenerAdapter sseListenerAdapter) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(sseListenerAdapter, new ChannelTopic(CHANNEL_NAME));
		return container;
	}

	@Bean
	public MessageListener sseMessageListener(ObjectMapper objectMapper) {
		return (Message message, byte[] pattern) -> {
			try {
				EventDto event = objectMapper.readValue(message.getBody(), EventDto.class);
				String sessionId = event.getSessionId();

				if (sseEmitterManager.has(sessionId)) {
					logger.info("[TRACE-5] Pub/Sub received, forwarding to SSE - type: {}, sessionId: {}",
							event.getType(), sessionId);
					sseEmitterManager.send(sessionId, event);
				}
			} catch (IOException e) {
				logger.error("[TRACE-5] Failed to deserialize ws-channel message: {} | raw: {}", e.getMessage(),
						new String(message.getBody()));
			}
		};
	}

	@Bean
	public MessageListenerAdapter sseListenerAdapter(MessageListener sseMessageListener) {
		return new MessageListenerAdapter(sseMessageListener, "onMessage");
	}
}
