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
