package idv.hzm.app.bot.config;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;
import idv.hzm.app.bot.entity.MessageChunk;
import idv.hzm.app.bot.repo.MessageChunkRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 新版 SSE Session 管理器：每個 session 一條 BlockingQueue，
 * 由共用 FixedThreadPool 中的單一線程負責消費並透過 Reactor Sink 推送至前端。
 * 確保 100% 不亂序、不重複。
 */
@Component
public class SseSessionManager {

	private static final Logger logger = LoggerFactory.getLogger(SseSessionManager.class);
	private static final EventDto POISON_PILL = new EventDto();

	private final ExecutorService sseExecutor;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;
	private final MessageChunkRepository messageChunkRepository;

	/** sessionId → 待發送佇列 */
	private final ConcurrentHashMap<String, BlockingQueue<EventDto>> queues = new ConcurrentHashMap<>();

	/** sessionId → Reactor Sink (SSE 輸出端) */
	private final ConcurrentHashMap<String, Sinks.Many<EventDto>> sinks = new ConcurrentHashMap<>();

	public SseSessionManager(@Qualifier("sseExecutor") ExecutorService sseExecutor,
			ReactiveRedisTemplate<String, String> redisTemplate,
			ObjectMapper objectMapper,
			MessageChunkRepository messageChunkRepository) {
		this.sseExecutor = sseExecutor;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.messageChunkRepository = messageChunkRepository;
	}

	/**
	 * 前端訂閱 SSE 時呼叫，回傳 Flux 供 Controller 使用。
	 */
	public Flux<EventDto> subscribe(String sessionId) {
		BlockingQueue<EventDto> queue = queues.computeIfAbsent(sessionId, k -> new LinkedBlockingQueue<>());
		Sinks.Many<EventDto> sink = sinks.computeIfAbsent(sessionId, k -> {
			Sinks.Many<EventDto> s = Sinks.many().unicast().onBackpressureBuffer();
			// 啟動單線程消費者
			sseExecutor.submit(() -> drainQueue(sessionId, queue, s));
			return s;
		});

		return sink.asFlux()
				.doOnCancel(() -> {
					logger.info("[SSE-MGR] Subscriber cancelled: sessionId={}", sessionId);
					remove(sessionId);
				});
	}

	/**
	 * ResponseConsumer 呼叫此方法將事件推送至使用者的佇列。
	 */
	public void enqueue(String sessionId, EventDto event) {
		BlockingQueue<EventDto> queue = queues.get(sessionId);
		if (queue != null) {
			queue.offer(event);
		}
	}

	/**
	 * 檢查 sessionId 是否有活躍的連線（本機）。
	 */
	public boolean isLocalSession(String sessionId) {
		return sinks.containsKey(sessionId);
	}

	/**
	 * 清理 session 資源。
	 */
	public void remove(String sessionId) {
		queues.remove(sessionId);
		Sinks.Many<EventDto> sink = sinks.remove(sessionId);
		if (sink != null) {
			sink.tryEmitComplete();
		}
	}

	public Set<String> getConnectedSessions() {
		return sinks.keySet();
	}

	/**
	 * 從資料庫恢復遺漏的 chunks：根據 messageId 和 lastSeqIndex，
	 * 查詢後續的 chunks 並推入佇列。
	 */
	public void recoverMissedChunksFromDb(String sessionId, String messageId, int lastSeqIndex) {
		java.util.List<MessageChunk> missed = messageChunkRepository
				.findByMessageIdAndSeqIndexGreaterThanOrderBySeqIndexAsc(messageId, lastSeqIndex);

		logger.info("[SSE-MGR] Recovering {} missed chunks from DB for sessionId={}, messageId={}, afterSeq={}",
				missed.size(), sessionId, messageId, lastSeqIndex);

		for (MessageChunk chunk : missed) {
			EventDto event = new EventDto();
			event.setSessionId(sessionId);
			event.setInteractionId(chunk.getInteractionId());
			event.setType(EventDto.TYPE_STREAM_CHUNK);
			event.setPayload(new MessagePayload(chunk.getContent()));
			enqueue(sessionId, event);
		}
	}

	private static final int RECOVERY_BATCH_SIZE = 500;

	/**
	 * 從 Last-Event-ID 恢復：讀取 stream:response 中該 ID 之後屬於此 sessionId 的訊息，
	 * 重新推入佇列以接續發送。限制每次最多讀取 RECOVERY_BATCH_SIZE 筆，避免大量資料一次載入。
	 */
	public void recoverFromLastEventId(String sessionId, String lastEventId) {
		if (lastEventId == null || lastEventId.isBlank()) return;

		logger.info("[SSE-MGR] Recovering from Last-Event-ID={} for sessionId={}", lastEventId, sessionId);

		// 從 lastEventId 的下一筆開始讀取，限制筆數
		redisTemplate.opsForStream()
				.read(StreamReadOptions.empty().count(RECOVERY_BATCH_SIZE),
						StreamOffset.create(RedisStreamConfig.RESPONSE_STREAM, ReadOffset.from(lastEventId)))
				.filter(record -> {
					MapRecord<String, Object, Object> r = (MapRecord<String, Object, Object>) (MapRecord<?, ?, ?>) record;
					Object sid = r.getValue().get("sessionId");
					return sessionId.equals(sid);
				})
				.doOnNext(record -> {
					try {
						MapRecord<String, Object, Object> r = (MapRecord<String, Object, Object>) (MapRecord<?, ?, ?>) record;
						java.util.Map<Object, Object> value = r.getValue();
						String type = String.valueOf(value.get("type"));
						String content = String.valueOf(value.get("content"));
						String interactionId = String.valueOf(value.get("interactionId"));

						EventDto event = new EventDto();
						event.setSessionId(sessionId);
						event.setInteractionId(interactionId);

						switch (type) {
							case "END":
								event.setType(EventDto.TYPE_STREAM_END);
								MessagePayload payload = new MessagePayload();
								String sourcesStr = String.valueOf(value.get("sources"));
								if (sourcesStr != null && !sourcesStr.equals("null")) {
									try {
										payload.setSources(objectMapper.readValue(sourcesStr,
												objectMapper.getTypeFactory().constructCollectionType(
														java.util.List.class, java.util.Map.class)));
									} catch (Exception ignored) {}
								}
								event.setPayload(payload);
								break;
							case "PAUSED":
								event.setType(EventDto.TYPE_STREAM_PAUSED);
								break;
							case "CANCELLED":
								event.setType(EventDto.TYPE_STREAM_CANCELLED);
								break;
							default:
								event.setType(EventDto.TYPE_STREAM_CHUNK);
								event.setPayload(new MessagePayload(content));
								break;
						}

						enqueue(sessionId, event);
					} catch (Exception e) {
						logger.warn("[SSE-MGR] Failed to recover record: {}", e.getMessage());
					}
				})
				.subscribe();
	}

	/**
	 * 單線程消費佇列，保證絕對順序。
	 */
	private void drainQueue(String sessionId, BlockingQueue<EventDto> queue, Sinks.Many<EventDto> sink) {
		logger.info("[SSE-MGR] Drain thread started for sessionId={}", sessionId);
		try {
			while (!Thread.currentThread().isInterrupted()) {
				EventDto event = queue.poll(50, TimeUnit.MILLISECONDS);
				if (event == null) {
					// 檢查 sink 是否已被關閉
					if (!sinks.containsKey(sessionId)) break;
					continue;
				}
				if (event == POISON_PILL) break;

				Sinks.EmitResult result = sink.tryEmitNext(event);
				if (result.isFailure()) {
					logger.warn("[SSE-MGR] Failed to emit event for sessionId={}, result={}", sessionId, result);
					break;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			remove(sessionId);
			logger.info("[SSE-MGR] Drain thread ended for sessionId={}", sessionId);
		}
	}
}
