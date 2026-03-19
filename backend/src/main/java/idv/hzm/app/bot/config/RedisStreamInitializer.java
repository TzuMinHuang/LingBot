package idv.hzm.app.bot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamInitializer.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private NodeIdentity nodeIdentity;

    @Override
    public void run(String... args) {
        // stream:request — 共用 consumer group
        initializeStream(RedisStreamConfig.REQUEST_STREAM, RedisStreamConfig.REQUEST_CONSUMER_GROUP);

        // stream:response — per-node consumer group（廣播模式）
        String responseGroup = nodeIdentity.getResponseConsumerGroup();
        initializeStream(RedisStreamConfig.RESPONSE_STREAM, responseGroup);

        // 清理已失效節點的 consumer group
        cleanupStaleResponseGroups();

        // 初始化心跳
        nodeIdentity.refreshHeartbeat();
    }

    private void initializeStream(String streamKey, String groupName) {
        try {
            redisTemplate.execute((RedisConnection connection) -> {
                try {
                    connection.streamCommands().xInfo(streamKey.getBytes());
                } catch (Exception e) {
                    logger.info("Stream {} does not exist, creating it.", streamKey);
                }

                try {
                    connection.streamCommands().xGroupCreate(streamKey.getBytes(), groupName, ReadOffset.from("0"), true);
                    logger.info("Created consumer group {} for stream {}", groupName, streamKey);
                } catch (Exception e) {
                    if (e.getMessage().contains("BUSYGROUP")) {
                        logger.info("Consumer group {} already exists for stream {}", groupName, streamKey);
                    } else {
                        logger.error("Failed to create consumer group: {}", e.getMessage());
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.error("Error during Redis Stream initialization: {}", e.getMessage());
        }
    }

    private void cleanupStaleResponseGroups() {
        try {
            redisTemplate.execute((RedisConnection connection) -> {
                try {
                    var groups = connection.streamCommands()
                            .xInfoGroups(RedisStreamConfig.RESPONSE_STREAM.getBytes());
                    if (groups == null) return null;

                    for (var group : groups) {
                        String groupName = group.groupName();
                        if (groupName == null || !groupName.startsWith("response-cg-")) continue;

                        // 從 group name 取出 instanceId
                        String instanceId = groupName.substring("response-cg-".length());
                        String heartbeatKey = "node:heartbeat:" + instanceId;

                        // 跳過自身
                        if (instanceId.equals(nodeIdentity.getInstanceId())) continue;

                        // 檢查心跳 key 是否存在
                        if (!Boolean.TRUE.equals(redisTemplate.hasKey(heartbeatKey))) {
                            logger.info("[CLEANUP] Removing stale consumer group: {} (heartbeat expired)", groupName);
                            connection.streamCommands().xGroupDestroy(
                                    RedisStreamConfig.RESPONSE_STREAM.getBytes(), groupName);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("[CLEANUP] Failed to cleanup stale response groups: {}", e.getMessage());
                }
                return null;
            });
        } catch (Exception e) {
            logger.warn("[CLEANUP] Error during stale group cleanup: {}", e.getMessage());
        }
    }
}
