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
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.client.AnythingLLMClient;
import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;
import idv.hzm.app.bot.service.ChatHistoryService;
import idv.hzm.app.bot.service.ChatRedisService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class BotConsumer {

	private static final Logger logger = LoggerFactory.getLogger(BotConsumer.class);
	private static final String INSTANCE_ID = java.util.UUID.randomUUID().toString().substring(0, 8);

	@Autowired
	private AnythingLLMClient anythingLLMClient;

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private RedisStreamConfig redisStreamConfig;

	@Autowired
	private ChatRedisService chatRedisService;

	@PostConstruct
	public void subscribe() {
		// Group 創建交由 RedisStreamInitializer 處理，此處僅啟動消費者
		int count = redisStreamConfig.getConsumerCount();
		for (int i = 1; i <= count; i++) {
			String consumerName = "bot-consumer-" + INSTANCE_ID + "-" + i;
			startConsumer(consumerName);
		}
		logger.info("[RE-WORKER] Started {} reactive consumers on stream: {}", count,
				RedisStreamConfig.BOT_INCOMING_STREAM);
	}

	private void startConsumer(String consumerName) {
		streamReceiver.receive(Consumer.from(RedisStreamConfig.CONSUMER_GROUP, consumerName),
				StreamOffset.create(RedisStreamConfig.BOT_INCOMING_STREAM, ReadOffset.lastConsumed()))
				.flatMap(this::processRecord)
				.subscribe();
	}

	private Mono<Void> processRecord(MapRecord<String, String, String> record) {
		Map<String, String> value = record.getValue();
		String sessionId = value.get("sessionId");
		String content = value.get("content");
		String interactionId = value.get("interactionId");

		if (sessionId == null || content == null || interactionId == null) {
			logger.warn("[RE-WORKER] Invalid stream record, skipping: {}", value);
			return ack(record);
		}

		logger.info("[RE-WORKER] Processing - sessionId: {}, interactionId: {}", sessionId, interactionId);

		StringBuilder fullResponse = new StringBuilder();

		return Mono.just(new EventDto())
				.doOnNext(e -> {
					e.setSessionId(sessionId);
					e.setInteractionId(interactionId);
					e.setType(EventDto.TYPE_PROCESSING_START);
				})
				.flatMap(this::publishDualMode)
				.thenMany(anythingLLMClient.chatStream(content, sessionId))
				.flatMap(chunkMap -> handleChunk(chunkMap, sessionId, interactionId, fullResponse))
				.then(Mono.defer(() -> {
					// 釋放鎖並發送 XACK
					chatRedisService.releaseRequestLock(sessionId);
					return ack(record);
				}))
				.onErrorResume(e -> {
					logger.error("[RE-WORKER] Error processing sessionId {}: {}", sessionId, e.getMessage());
					chatRedisService.releaseRequestLock(sessionId);

					// 實作 DLQ：如果失敗，嘗試發送到 DLQ
					return moveToDLQ(record, e.getMessage())
							.then(ack(record));
				})
				.then();
	}

	private Mono<Void> handleChunk(Map<String, Object> chunkMap, String sessionId, String interactionId, StringBuilder fullResponse) {
	    boolean isClose = Boolean.TRUE.equals(chunkMap.get("close"));
	    
	    // 1. 提取 sources (不論是不是結束 chunk，只要有就拿)
	    List<Map<String, Object>> sources = (List<Map<String, Object>>) chunkMap.get("sources");

	    // 2. 處理文字部分
	    String chunkText = (String) chunkMap.get("textResponse");
	    if (chunkText != null && !chunkText.isEmpty()) {
	        fullResponse.append(chunkText);
	    }

	  // 3. 構造 EventDto
	 // 1. 先發 STREAM_CHUNK
	    if (chunkText != null || (sources != null && !sources.isEmpty())) {
	        EventDto event = new EventDto();
	        event.setSessionId(sessionId);
	        event.setInteractionId(interactionId);
	        event.setType(EventDto.TYPE_STREAM_CHUNK);
	        MessagePayload payload = new MessagePayload(chunkText);
	        payload.setSources(sources);
	        event.setPayload(payload);
	        publishDualMode(event).subscribe();
	    }

	    // 2. 最後一個 chunk 才發 STREAM_END
	    if (isClose) {
	        EventDto endEvent = new EventDto();
	        endEvent.setSessionId(sessionId);
	        endEvent.setInteractionId(interactionId);
	        endEvent.setType(EventDto.TYPE_STREAM_END);
	        MessagePayload payload = new MessagePayload();
	        payload.setSources(sources); // 最終 sources
	        endEvent.setPayload(payload);
	        saveHistoryAsync(sessionId, interactionId, fullResponse.toString(), sources);
	        publishDualMode(endEvent).subscribe();
	    }

	    return Mono.empty();
	}

	// 輔助方法：優化歷史紀錄保存
	private void saveHistoryAsync(String sessionId, String interactionId, String content, List<Map<String, Object>> sources) {
	    Mono.fromRunnable(() -> chatHistoryService.saveAssistantMessage(sessionId, interactionId, content, sources))
	            .subscribeOn(Schedulers.boundedElastic())
	            .subscribe();
	}

	private Mono<Void> publishDualMode(EventDto event) {
		String sessionId = event.getSessionId();
		String streamKey = "stream:chat:res:" + sessionId;

		try {
			String json = objectMapper.writeValueAsString(event);

			// Mode A: Pub/Sub
			Mono<Long> pubSub = reactiveRedisTemplate.convertAndSend("ws-channel", json);

			// Mode B: Session Stream
			Map<String, String> record = Map.of("payload", json, "ts", String.valueOf(System.currentTimeMillis()));
			Mono<String> sessionStream = reactiveRedisTemplate.opsForStream()
					.add(streamKey, record)
					.flatMap(id -> {
						// 修剪 Session Stream，避免過長 (例如只留最後 50 筆)
						return reactiveRedisTemplate.opsForStream().trim(streamKey, 50).thenReturn(id.getValue());
					});

			return Mono.zip(pubSub, sessionStream)
					.doOnNext(t -> logger.debug("[DUAL-MODE] Published sessionId: {}, type: {}", sessionId,
							event.getType()))
					.then();

		} catch (Exception e) {
			logger.error("[DUAL-MODE] Failed to serialize or publish: {}", e.getMessage());
			return Mono.empty();
		}
	}

	private Mono<Void> ack(MapRecord<String, String, String> record) {
		return reactiveRedisTemplate.opsForStream()
				.acknowledge(RedisStreamConfig.CONSUMER_GROUP, record)
				.then(reactiveRedisTemplate.opsForStream().trim(RedisStreamConfig.BOT_INCOMING_STREAM, 100).then()); // 修編
																														// Stream，保留最後
																														// 100
																														// 筆，防止
																														// XLEN
																														// 無效增長
	}

	private Mono<Void> moveToDLQ(MapRecord<String, String, String> record, String error) {
		Map<String, String> dlqValue = new java.util.HashMap<>(record.getValue());
		dlqValue.put("error", error);
		dlqValue.put("originalId", record.getId().getValue());
		dlqValue.put("failedAt", String.valueOf(System.currentTimeMillis()));

		return reactiveRedisTemplate.opsForStream()
				.add(RedisStreamConfig.BOT_DLQ_STREAM, dlqValue)
				.doOnNext(id -> logger.warn("[DLQ] Message moved to DLQ: originalId={}, dlqId={}",
						record.getId().getValue(), id))
				.then();
	}
}
