package idv.hzm.app.bot.config;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * 定期收集 Redis Stream 和 SSE 管道的健康指標，透過 Micrometer 暴露。
 */
@Component
public class StreamMetricsCollector {

	private static final Logger logger = LoggerFactory.getLogger(StreamMetricsCollector.class);

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private SseSessionManager sseSessionManager;

	@Autowired
	private NodeIdentity nodeIdentity;

	@Autowired
	private MeterRegistry meterRegistry;

	private final AtomicLong requestLag = new AtomicLong(0);
	private final AtomicLong responseLag = new AtomicLong(0);
	private final AtomicLong requestPelSize = new AtomicLong(0);
	private final AtomicLong activeSessions = new AtomicLong(0);
	private final ConcurrentHashMap<String, AtomicLong> consumerIdleSeconds = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> consumerLastProcessed = new ConcurrentHashMap<>();

	@PostConstruct
	public void registerGauges() {
		meterRegistry.gauge("stream.request.lag", requestLag);
		meterRegistry.gauge("stream.response.lag", responseLag);
		meterRegistry.gauge("stream.request.pel.size", requestPelSize);
		meterRegistry.gauge("sse.active.sessions", activeSessions);
	}

	/**
	 * 記錄消費者最後處理時間，供 idle time 計算。
	 */
	public void recordConsumerActivity(String consumerName) {
		consumerLastProcessed.put(consumerName, System.currentTimeMillis());
		consumerIdleSeconds.computeIfAbsent(consumerName, name -> {
			AtomicLong idle = new AtomicLong(0);
			meterRegistry.gauge("consumer.idle.seconds", Tags.of("consumer", name), idle);
			return idle;
		});
	}

	@Scheduled(fixedDelay = 15000)
	public void collectMetrics() {
		// SSE active sessions (local, no async needed)
		activeSessions.set(sseSessionManager.getConnectedSessions().size());

		// Stream lag and PEL via XINFO GROUPS
		collectStreamLag(RedisStreamConfig.REQUEST_STREAM, RedisStreamConfig.REQUEST_CONSUMER_GROUP, requestLag);
		collectStreamLag(RedisStreamConfig.RESPONSE_STREAM, nodeIdentity.getResponseConsumerGroup(), responseLag);
		collectPelSize();

		// Consumer idle time
		long now = System.currentTimeMillis();
		consumerLastProcessed.forEach((name, lastTime) -> {
			AtomicLong idle = consumerIdleSeconds.get(name);
			if (idle != null) {
				idle.set((now - lastTime) / 1000);
			}
		});
	}

	private void collectStreamLag(String streamKey, String groupName, AtomicLong gauge) {
		reactiveRedisTemplate.execute(conn -> conn.streamCommands()
						.xInfoGroups(ByteBuffer.wrap(streamKey.getBytes())))
				.filter(group -> groupName.equals(group.groupName()))
				.next()
				.subscribe(
						group -> {
							long pending = group.pendingCount() != null ? group.pendingCount() : 0;
							gauge.set(pending);
						},
						error -> logger.debug("[METRICS] Failed to read lag for {}/{}: {}",
								streamKey, groupName, error.getMessage()));
	}

	private void collectPelSize() {
		reactiveRedisTemplate.opsForStream()
				.pending(RedisStreamConfig.REQUEST_STREAM, RedisStreamConfig.REQUEST_CONSUMER_GROUP)
				.subscribe(
						summary -> requestPelSize.set(summary.getTotalPendingMessages()),
						error -> logger.debug("[METRICS] Failed to read PEL size: {}", error.getMessage()));
	}
}
