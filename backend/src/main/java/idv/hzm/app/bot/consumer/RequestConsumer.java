package idv.hzm.app.bot.consumer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.client.AnythingLLMClient;
import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.entity.MessageStatus;
import idv.hzm.app.bot.service.ChatHistoryService;
import idv.hzm.app.bot.service.ChatRedisService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 專職監聽 stream:request，呼叫 LLM 服務，
 * 將結果餵入 LlmThrottlingBuffer 合併後寫入 stream:response。
 */
@Component
public class RequestConsumer {

	private static final Logger logger = LoggerFactory.getLogger(RequestConsumer.class);
	private static final String INSTANCE_ID = java.util.UUID.randomUUID().toString().substring(0, 8);

	@Autowired
	private AnythingLLMClient anythingLLMClient;

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private StreamReceiver<String, MapRecord<String, String, String>> streamReceiver;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private RedisStreamConfig redisStreamConfig;

	@Autowired
	private ChatRedisService chatRedisService;

	@Autowired
	@Qualifier("throttleScheduler")
	private ScheduledExecutorService throttleScheduler;

	@org.springframework.beans.factory.annotation.Value("${app.throttle-buffer-capacity:512}")
	private int throttleBufferCapacity;

	@Autowired
	private idv.hzm.app.bot.config.StreamMetricsCollector streamMetricsCollector;

	@Autowired
	private idv.hzm.app.bot.repo.MessageChunkRepository messageChunkRepository;

	@EventListener(ApplicationReadyEvent.class)
	public void subscribe() {
		int count = redisStreamConfig.getConsumerCount();
		for (int i = 1; i <= count; i++) {
			String consumerName = "req-consumer-" + INSTANCE_ID + "-" + i;
			startConsumer(consumerName);
		}
		logger.info("[REQ-CONSUMER] Started {} consumers on stream: {}", count, RedisStreamConfig.REQUEST_STREAM);
	}

	private void startConsumer(String consumerName) {
		streamReceiver.receive(
				Consumer.from(RedisStreamConfig.REQUEST_CONSUMER_GROUP, consumerName),
				StreamOffset.create(RedisStreamConfig.REQUEST_STREAM, ReadOffset.lastConsumed()))
				.flatMap(this::processRecord)
				.subscribe();
	}

	@SuppressWarnings("unchecked")
	Mono<Void> processRecord(MapRecord<String, String, String> record) {
		Map<String, String> value = record.getValue();
		String sessionId = value.get("sessionId");
		String content = value.get("content");
		String interactionId = value.get("interactionId");

		if (sessionId == null || content == null || interactionId == null) {
			logger.warn("[REQ-CONSUMER] Invalid stream record, skipping: {}", value);
			return ack(record);
		}

		logger.info("[REQ-CONSUMER] Processing - sessionId: {}, interactionId: {}", sessionId, interactionId);
		streamMetricsCollector.recordConsumerActivity("req-consumer-" + INSTANCE_ID);

		// 發送 PROCESSING_START 至 stream:response
		Map<String, String> startRecord = Map.of(
				"sessionId", sessionId,
				"interactionId", interactionId,
				"type", "PROCESSING_START",
				"content", "");
		reactiveRedisTemplate.opsForStream()
				.add(RedisStreamConfig.RESPONSE_STREAM, startRecord)
				.subscribe();

		// 預先產生 assistant message ID，供 chunk 持久化使用
		String assistantMessageId = java.util.UUID.randomUUID().toString();

		StringBuilder fullResponse = new StringBuilder();
		LlmThrottlingBuffer throttle = new LlmThrottlingBuffer(sessionId, interactionId, assistantMessageId,
				reactiveRedisTemplate, throttleScheduler, throttleBufferCapacity, messageChunkRepository);

		// 控制信號輪詢：延遲 1 秒後開始，每 1 秒檢查 Redis 是否有 PAUSE/CANCEL 信號
		// 延遲啟動避免對短回應產生不必要的 Redis 查詢
		AtomicReference<String> controlSignalRef = new AtomicReference<>();
		Flux<String> controlPoller = Flux.interval(Duration.ofSeconds(1), Duration.ofSeconds(1))
				.publishOn(Schedulers.boundedElastic())
				.mapNotNull(tick -> chatRedisService.consumeControlSignal(sessionId))
				.doOnNext(controlSignalRef::set)
				.take(1);

		return anythingLLMClient.chatStream(content, sessionId)
				.takeUntilOther(controlPoller)
				.doOnNext(chunkMap -> {
					boolean isClose = Boolean.TRUE.equals(chunkMap.get("close"));
					String chunkText = (String) chunkMap.get("textResponse");
					List<Map<String, Object>> sources = (List<Map<String, Object>>) chunkMap.get("sources");

					if (chunkText != null && !chunkText.isEmpty()) {
						fullResponse.append(chunkText);
						throttle.append(chunkText);
					}

					if (isClose) {
						throttle.flushAndEnd(sources);
						saveHistoryAsync(sessionId, interactionId, assistantMessageId,
								fullResponse.toString(), sources, MessageStatus.COMPLETED);
					}
				})
				.then(Mono.defer(() -> {
					String signal = controlSignalRef.get();
					if (signal != null) {
						// 串流被使用者中斷
						logger.info("[REQ-CONSUMER] Control signal '{}' received for sessionId: {}", signal, sessionId);
						throttle.close();
						MessageStatus status = "CANCEL".equals(signal) ? MessageStatus.CANCELLED : MessageStatus.PAUSED;
						saveHistoryAsync(sessionId, interactionId, assistantMessageId,
								fullResponse.toString(), null, status);
						sendControlEndMarker(sessionId, interactionId, signal);
					}
					chatRedisService.releaseRequestLock(sessionId);
					return ack(record);
				}))
				.onErrorResume(e -> {
					logger.error("[REQ-CONSUMER] Error processing sessionId {}: {}", sessionId, e.getMessage());
					throttle.close();
					chatRedisService.releaseRequestLock(sessionId);
					return moveToDLQ(record, e.getMessage()).then(ack(record));
				})
				.then();
	}

	private void saveHistoryAsync(String sessionId, String interactionId, String messageId,
			String content, List<Map<String, Object>> sources, MessageStatus status) {
		Mono.fromRunnable(() -> chatHistoryService.saveAssistantMessage(
					sessionId, interactionId, messageId, content, sources, status))
				.subscribeOn(Schedulers.boundedElastic())
				.subscribe();
	}

	private void sendControlEndMarker(String sessionId, String interactionId, String signal) {
		String type = "CANCEL".equals(signal) ? "CANCELLED" : "PAUSED";
		Map<String, String> endRecord = Map.of(
				"sessionId", sessionId,
				"interactionId", interactionId,
				"type", type,
				"content", "");
		reactiveRedisTemplate.opsForStream()
				.add(RedisStreamConfig.RESPONSE_STREAM, endRecord)
				.subscribe(id -> logger.info("[REQ-CONSUMER] Sent {} marker for sessionId={}", type, sessionId));
	}

	Mono<Void> ack(MapRecord<String, String, String> record) {
		return reactiveRedisTemplate.opsForStream()
				.acknowledge(RedisStreamConfig.REQUEST_CONSUMER_GROUP, record)
				.then(reactiveRedisTemplate.opsForStream()
						.trim(RedisStreamConfig.REQUEST_STREAM, RedisStreamConfig.STREAM_MAXLEN).then());
	}

	Mono<Void> moveToDLQ(MapRecord<String, String, String> record, String error) {
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
