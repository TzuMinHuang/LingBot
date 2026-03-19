package idv.hzm.app.bot.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 每個 Backend 節點的唯一識別，用於 per-node consumer group 和心跳。
 */
@Component
public class NodeIdentity {

	private static final Logger logger = LoggerFactory.getLogger(NodeIdentity.class);
	private static final Duration HEARTBEAT_TTL = Duration.ofMinutes(5);

	private final String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);

	@Autowired
	private StringRedisTemplate redisTemplate;

	public String getInstanceId() {
		return instanceId;
	}

	public String getResponseConsumerGroup() {
		return "response-cg-" + instanceId;
	}

	public String getHeartbeatKey() {
		return "node:heartbeat:" + instanceId;
	}

	@Scheduled(fixedDelay = 60000)
	public void refreshHeartbeat() {
		redisTemplate.opsForValue().set(getHeartbeatKey(), String.valueOf(System.currentTimeMillis()), HEARTBEAT_TTL);
		logger.debug("[NODE] Heartbeat refreshed for instanceId={}", instanceId);
	}
}
