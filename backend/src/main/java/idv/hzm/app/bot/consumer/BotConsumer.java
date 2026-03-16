package idv.hzm.app.bot.consumer;

import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.client.AnythingLLMClient;
import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.config.SseRedisSubscriber;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;
import idv.hzm.app.bot.service.ChatHistoryService;
import idv.hzm.app.bot.service.ChatRedisService;

@Component
public class BotConsumer implements StreamListener<String, MapRecord<String, String, String>> {

	private static final Logger logger = LoggerFactory.getLogger(BotConsumer.class);
	private static final String INSTANCE_ID = java.util.UUID.randomUUID().toString().substring(0, 8);

	@Autowired
	private AnythingLLMClient anythingLLMClient;

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;

	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StreamMessageListenerContainer<String, MapRecord<String, String, String>> streamMessageListenerContainer;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private RedisStreamConfig redisStreamConfig;

	@Autowired
	private ChatRedisService chatRedisService;

	@PostConstruct
	public void subscribe() {
		try {
			// 確保 stream 存在（若 Redis 重建後 stream 不存在，createGroup 會失敗）
			if (!Boolean.TRUE.equals(redisTemplate.hasKey(RedisStreamConfig.BOT_INCOMING_STREAM))) {
				redisTemplate.opsForStream().add(RedisStreamConfig.BOT_INCOMING_STREAM,
						java.util.Map.of("init", "true"));
				logger.info("[TRACE-1] Stream created: {}", RedisStreamConfig.BOT_INCOMING_STREAM);
			}
			redisTemplate.opsForStream().createGroup(RedisStreamConfig.BOT_INCOMING_STREAM, ReadOffset.latest(),
					RedisStreamConfig.CONSUMER_GROUP);
			logger.info("[TRACE-1] Consumer group created: {}", RedisStreamConfig.CONSUMER_GROUP);
		} catch (Exception e) {
			// BUSYGROUP 可能被包在多層異常中，需遍歷 cause chain
			Throwable cause = e;
			boolean isBusyGroup = false;
			while (cause != null) {
				if (cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP")) {
					isBusyGroup = true;
					break;
				}
				cause = cause.getCause();
			}
			if (isBusyGroup) {
				logger.info("[TRACE-1] Consumer group already exists (OK)");
			} else {
				throw new RuntimeException("Failed to create consumer group", e);
			}
		}

		int count = redisStreamConfig.getConsumerCount();
		for (int i = 1; i <= count; i++) {
			String consumerName = "bot-consumer-" + INSTANCE_ID + "-" + i;
			streamMessageListenerContainer.receive(Consumer.from(RedisStreamConfig.CONSUMER_GROUP, consumerName),
					StreamOffset.create(RedisStreamConfig.BOT_INCOMING_STREAM, ReadOffset.lastConsumed()), this);
		}
		logger.info("[TRACE-1] Subscribed {} consumers to stream: {}", count,
				RedisStreamConfig.BOT_INCOMING_STREAM);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onMessage(MapRecord<String, String, String> record) {
		Map<String, String> value = record.getValue();
		String sessionId = value.get("sessionId");
		String content = value.get("content");
		String interactionId = value.get("interactionId");

		if (sessionId == null || content == null || interactionId == null) {
			logger.warn("[TRACE-2] Invalid stream record, missing required fields: {}", value);
			redisTemplate.opsForStream().acknowledge(RedisStreamConfig.CONSUMER_GROUP, record);
			return;
		}

		logger.info("[TRACE-2] Stream message received - sessionId: {}, interactionId: {}", sessionId, interactionId);

		// Accumulate full response for DB persistence
		StringBuilder fullResponse = new StringBuilder();

		try {
			// 通知前端：消費者已取得訊息，開始處理
			EventDto startEvent = new EventDto();
			startEvent.setSessionId(sessionId);
			startEvent.setType(EventDto.TYPE_PROCESSING_START);
			publish(startEvent);

			logger.info("[TRACE-3] Calling AnythingLLM - sessionId: {}", sessionId);
			anythingLLMClient.chatStream(content, sessionId).doOnNext(event -> {
				boolean isClose = Boolean.TRUE.equals(event.get("close"));

				if (isClose) {
					// close:true → STREAM_END with sources
					List<Map<String, Object>> sources = (List<Map<String, Object>>) event.get("sources");

					MessagePayload endPayload = new MessagePayload();
					endPayload.setSources(sources);

					EventDto end = new EventDto();
					end.setSessionId(sessionId);
					end.setType(EventDto.TYPE_STREAM_END);
					end.setPayload(endPayload);
					publish(end);

					// Persist assistant message to DB
					chatHistoryService.saveAssistantMessage(
							sessionId, interactionId, fullResponse.toString(), sources);

					logger.info("[TRACE-3] Stream END - sessionId: {}, responseLength: {}",
							sessionId, fullResponse.length());
				} else {
					// Token chunk → STREAM_CHUNK
					String chunk = (String) event.get("textResponse");
					if (chunk == null || chunk.isEmpty())
						return;

					fullResponse.append(chunk);

					MessagePayload chunkPayload = new MessagePayload(chunk);
					EventDto chunkEvent = new EventDto();
					chunkEvent.setSessionId(sessionId);
					chunkEvent.setType(EventDto.TYPE_STREAM_CHUNK);
					chunkEvent.setPayload(chunkPayload);
					publish(chunkEvent);
				}
			}).doOnError(e -> logger.error("Stream error for sessionId {}: {}", sessionId, e.getMessage())).blockLast();
		} catch (Exception e) {
			logger.error("Error processing stream message for sessionId {}: {}", sessionId, e.getMessage(), e);
		} finally {
			// 處理完畢（不論成功失敗）釋放鎖
			chatRedisService.releaseRequestLock(sessionId);
			redisTemplate.opsForStream().acknowledge(RedisStreamConfig.CONSUMER_GROUP, record);
		}
	}

	private void publish(EventDto event) {
		try {
			String json = objectMapper.writeValueAsString(event);
			stringRedisTemplate.convertAndSend(SseRedisSubscriber.CHANNEL_NAME, json);
			logger.info("[TRACE-4] Published to Pub/Sub - type: {}, sessionId: {}", event.getType(),
					event.getSessionId());
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize event for sessionId {}: {}", event.getSessionId(), e.getMessage());
		}
	}
}
