package idv.hzm.app.bot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisStreamInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(RedisStreamInitializer.class);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void run(String... args) {
        initializeStream(RedisStreamConfig.BOT_INCOMING_STREAM, RedisStreamConfig.CONSUMER_GROUP);
    }

    private void initializeStream(String streamKey, String groupName) {
        try {
            redisTemplate.execute((RedisConnection connection) -> {
                try {
                    // Check if stream exists by trying to get its info
                    connection.streamCommands().xInfo(streamKey.getBytes());
                } catch (Exception e) {
                    // If stream doesn't exist, create it with a dummy entry and then delete it
                    logger.info("Stream {} does not exist, creating it.", streamKey);
                    // Use a more robust way to create stream if needed, 
                    // but usually XGROUP CREATE with MKSTREAM works.
                }

                try {
                    // Create consumer group with MKSTREAM option
                    connection.streamCommands().xGroupCreate(streamKey.getBytes(), groupName, org.springframework.data.redis.connection.stream.ReadOffset.from("0"), true);
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
}
