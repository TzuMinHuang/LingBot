package idv.hzm.app.bot.consumer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.entity.MessageChunk;
import idv.hzm.app.bot.repo.MessageChunkRepository;

/**
 * 雙層節流 Buffer：累積 LLM 碎塊，滿 100 字元或 50ms 後 flush 至 stream:response。
 * 每個 RequestConsumer 處理一筆請求時建立一個實例，不可重複使用。
 * 使用共享的 ScheduledExecutorService，避免每個實例建立獨立線程。
 * 使用有界佇列（ArrayBlockingQueue）防止記憶體無限成長。
 */
public class LlmThrottlingBuffer {

	private static final Logger logger = LoggerFactory.getLogger(LlmThrottlingBuffer.class);

	private static final int CHAR_THRESHOLD = 100;
	private static final long TIME_THRESHOLD_MS = 50;
	private static final int DEFAULT_CAPACITY = 512;

	private final String sessionId;
	private final String interactionId;
	private final String messageId;
	private final ReactiveRedisTemplate<String, String> redisTemplate;
	private final MessageChunkRepository chunkRepository;
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final ArrayBlockingQueue<String> chunks;
	private int bufferedCharCount = 0;
	private final ScheduledFuture<?> timerFuture;
	private volatile boolean ended = false;
	private long lastFlushTime = System.currentTimeMillis();
	private final AtomicInteger seqCounter = new AtomicInteger(0);

	public LlmThrottlingBuffer(String sessionId, String interactionId, String messageId,
			ReactiveRedisTemplate<String, String> redisTemplate,
			ScheduledExecutorService scheduler, MessageChunkRepository chunkRepository) {
		this(sessionId, interactionId, messageId, redisTemplate, scheduler, DEFAULT_CAPACITY, chunkRepository);
	}

	public LlmThrottlingBuffer(String sessionId, String interactionId, String messageId,
			ReactiveRedisTemplate<String, String> redisTemplate,
			ScheduledExecutorService scheduler, int capacity, MessageChunkRepository chunkRepository) {
		this.sessionId = sessionId;
		this.interactionId = interactionId;
		this.messageId = messageId;
		this.redisTemplate = redisTemplate;
		this.chunkRepository = chunkRepository;
		this.chunks = new ArrayBlockingQueue<>(capacity);
		this.timerFuture = scheduler.scheduleAtFixedRate(() -> {
			if (Thread.currentThread().isInterrupted()) return;
			synchronized (this) {
				if (!ended && bufferedCharCount > 0
						&& (System.currentTimeMillis() - lastFlushTime) >= TIME_THRESHOLD_MS) {
					flush();
				}
			}
		}, TIME_THRESHOLD_MS, TIME_THRESHOLD_MS, TimeUnit.MILLISECONDS);
	}

	/**
	 * 附加 LLM 回傳的文字碎塊，當累積超過 CHAR_THRESHOLD 時自動 flush。
	 * 佇列滿時採用 drop-oldest 策略。
	 */
	public synchronized void append(String text) {
		if (ended || text == null || text.isEmpty()) return;

		// drop-oldest overflow
		while (!chunks.offer(text)) {
			String dropped = chunks.poll();
			if (dropped != null) {
				bufferedCharCount -= dropped.length();
				logger.warn("[THROTTLE] Buffer overflow, dropped {} chars for sessionId={}", dropped.length(), sessionId);
			}
		}
		bufferedCharCount += text.length();

		if (bufferedCharCount >= CHAR_THRESHOLD) {
			flush();
		}
	}

	/**
	 * LLM 串流結束時呼叫：flush 剩餘文字，寫入 END 標記，取消定時任務。
	 */
	public synchronized void flushAndEnd(java.util.List<java.util.Map<String, Object>> sources) {
		if (ended) return;
		ended = true;
		timerFuture.cancel(true);

		// flush 剩餘文字
		if (bufferedCharCount > 0) {
			doFlush(drainToString());
		}

		// 寫入 END 標記
		String sourcesJson = "[]";
		if (sources != null) {
			try {
				sourcesJson = OBJECT_MAPPER.writeValueAsString(sources);
			} catch (Exception e) {
				logger.warn("Failed to serialize sources: {}", e.getMessage());
			}
		}

		Map<String, String> endRecord = Map.of(
				"sessionId", sessionId,
				"interactionId", interactionId,
				"type", "END",
				"content", "",
				"sources", sourcesJson);

		redisTemplate.opsForStream()
				.add(RedisStreamConfig.RESPONSE_STREAM, endRecord)
				.flatMap(id -> redisTemplate.opsForStream()
						.trim(RedisStreamConfig.RESPONSE_STREAM, RedisStreamConfig.STREAM_MAXLEN)
						.thenReturn(id))
				.subscribe(id -> logger.debug("[THROTTLE] END written for sessionId={}, id={}", sessionId, id.getValue()));
	}

	/**
	 * 異常或主動中斷時呼叫：立即取消定時任務（可中斷執行中的任務），flush 剩餘文字（不寫 END）。
	 * 此方法由 RequestConsumer 的 onErrorResume 或收到 PAUSE/CANCEL 控制信號時呼叫。
	 */
	public synchronized void close() {
		if (ended) return;
		ended = true;
		// cancel(true) 允許中斷正在執行的定時 flush，確保資源即時釋放
		timerFuture.cancel(true);
		logger.debug("[THROTTLE] Closed buffer for sessionId={}, flushing remaining {} chars", sessionId, bufferedCharCount);

		if (bufferedCharCount > 0) {
			doFlush(drainToString());
		}
	}

	private synchronized void flush() {
		if (bufferedCharCount == 0) return;
		String text = drainToString();
		lastFlushTime = System.currentTimeMillis();
		doFlush(text);
	}

	private String drainToString() {
		List<String> drained = new ArrayList<>();
		chunks.drainTo(drained);
		bufferedCharCount = 0;
		StringBuilder sb = new StringBuilder();
		for (String chunk : drained) {
			sb.append(chunk);
		}
		return sb.toString();
	}

	private void doFlush(String text) {
		if (text.isEmpty()) return;

		int seq = seqCounter.incrementAndGet();

		// 持久化 chunk 至資料庫
		if (chunkRepository != null && messageId != null) {
			try {
				MessageChunk chunk = new MessageChunk();
				chunk.setMessageId(messageId);
				chunk.setSessionId(sessionId);
				chunk.setInteractionId(interactionId);
				chunk.setSeqIndex(seq);
				chunk.setContent(text);
				chunk.setCreatedAt(Instant.now());
				chunkRepository.save(chunk);
			} catch (Exception e) {
				logger.warn("[THROTTLE] Failed to persist chunk seq={} for sessionId={}: {}", seq, sessionId, e.getMessage());
			}
		}

		Map<String, String> record = Map.of(
				"sessionId", sessionId,
				"interactionId", interactionId,
				"type", "CHUNK",
				"content", text);

		redisTemplate.opsForStream()
				.add(RedisStreamConfig.RESPONSE_STREAM, record)
				.flatMap(id -> redisTemplate.opsForStream()
						.trim(RedisStreamConfig.RESPONSE_STREAM, RedisStreamConfig.STREAM_MAXLEN)
						.thenReturn(id))
				.subscribe(id -> logger.debug("[THROTTLE] Flushed {} chars for sessionId={}, id={}", text.length(), sessionId, id.getValue()));
	}
}
