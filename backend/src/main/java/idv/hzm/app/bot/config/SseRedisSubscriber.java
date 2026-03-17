package idv.hzm.app.bot.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.dto.EventDto;

/**
 * Redis Pub/Sub → SSE bridge 訂閱 ws-channel，收到訊息後轉發給本實例持有的 SseEmitter
 */
@Component
public class SseRedisSubscriber implements CommandLineRunner {

	private static final Logger logger = LoggerFactory.getLogger(SseRedisSubscriber.class);

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private SseEmitterManager sseEmitterManager;

	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public void run(String... args) {
		reactiveRedisTemplate.listenToChannel("ws-channel")
				.doOnNext(msg -> {
					String rawJson = msg.getMessage();
					try {
						EventDto event = objectMapper.readValue(rawJson, EventDto.class);
						String sessionId = event.getSessionId();
						if (sessionId != null) {
							sseEmitterManager.send(sessionId, event);
						}
					} catch (Exception e) {
						logger.error("[RE-PUB] Failed to deserialize ws-channel message: {}", e.getMessage());
					}
				})
				.doOnError(err -> logger.error("[RE-PUB] Subscription error on ws-channel", err))
				.subscribe();
		
		logger.info("[RE-PUB] Subscribed to Redis channel: ws-channel");
	}
}
