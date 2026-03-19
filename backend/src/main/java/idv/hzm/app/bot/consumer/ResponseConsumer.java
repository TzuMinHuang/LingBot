package idv.hzm.app.bot.consumer;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.config.NodeIdentity;
import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.config.SseSessionManager;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;
import reactor.core.publisher.Mono;

/**
 * 監聽 stream:response（廣播模式），每個節點有自己的 consumer group，
 * 確保所有節點都收到所有回應訊息。檢查 isLocalSession 後推送至 SseSessionManager。
 */
@Component
public class ResponseConsumer {

	private static final Logger logger = LoggerFactory.getLogger(ResponseConsumer.class);

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;

	@Autowired
	private SseSessionManager sseSessionManager;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private NodeIdentity nodeIdentity;

	private String consumerGroup;

	@EventListener(ApplicationReadyEvent.class)
	public void subscribe() {
		consumerGroup = nodeIdentity.getResponseConsumerGroup();
		String consumerName = "res-consumer-" + nodeIdentity.getInstanceId();
		startConsumer(consumerName);
		logger.info("[RES-CONSUMER] Started consumer {} in group {} on stream: {}",
				consumerName, consumerGroup, RedisStreamConfig.RESPONSE_STREAM);
	}

	private void startConsumer(String consumerName) {
		streamReceiver.receive(
				Consumer.from(consumerGroup, consumerName),
				StreamOffset.create(RedisStreamConfig.RESPONSE_STREAM, ReadOffset.lastConsumed()))
				.flatMap(this::processRecord)
				.subscribe();
	}

	@SuppressWarnings("unchecked")
	private Mono<Void> processRecord(MapRecord<String, String, String> record) {
		Map<String, String> value = record.getValue();
		String sessionId = value.get("sessionId");
		String interactionId = value.get("interactionId");
		String type = value.get("type");
		String content = value.get("content");

		// 只處理本機持有連線的 session
		if (sessionId == null || !sseSessionManager.isLocalSession(sessionId)) {
			return ack(record);
		}

		logger.debug("[RES-CONSUMER] Dispatching type={} for sessionId={}", type, sessionId);

		EventDto event = new EventDto();
		event.setSessionId(sessionId);
		event.setInteractionId(interactionId);

		switch (type) {
			case "PROCESSING_START":
				event.setType(EventDto.TYPE_PROCESSING_START);
				break;
			case "CHUNK":
				event.setType(EventDto.TYPE_STREAM_CHUNK);
				event.setPayload(new MessagePayload(content));
				break;
			case "END":
				event.setType(EventDto.TYPE_STREAM_END);
				MessagePayload endPayload = new MessagePayload();
				String sourcesStr = value.get("sources");
				if (sourcesStr != null && !sourcesStr.isEmpty()) {
					try {
						endPayload.setSources(objectMapper.readValue(sourcesStr,
								objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)));
					} catch (Exception e) {
						logger.warn("[RES-CONSUMER] Failed to parse sources: {}", e.getMessage());
					}
				}
				event.setPayload(endPayload);
				break;
			case "PAUSED":
				event.setType(EventDto.TYPE_STREAM_PAUSED);
				break;
			case "CANCELLED":
				event.setType(EventDto.TYPE_STREAM_CANCELLED);
				break;
			default:
				logger.warn("[RES-CONSUMER] Unknown type: {}", type);
				return ack(record);
		}

		sseSessionManager.enqueue(sessionId, event);
		return ack(record);
	}

	private Mono<Void> ack(MapRecord<String, String, String> record) {
		return reactiveRedisTemplate.opsForStream()
				.acknowledge(consumerGroup, record)
				.then();
	}
}
