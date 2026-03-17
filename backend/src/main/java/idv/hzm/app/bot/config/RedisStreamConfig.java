package idv.hzm.app.bot.config;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer.StreamMessageListenerContainerOptions;

@Configuration
public class RedisStreamConfig {

	public static final String BOT_INCOMING_STREAM = "stream:bot:incoming";
	public static final String BOT_EXECUTE_STREAM = "stream:bot:execute_request";
	public static final String BOT_RESPOND_STREAM = "stream:bot:execute_respond";
	public static final String CONSUMER_GROUP = "bot-consumer-group";

	@Value("${app.consumer-count:8}")
	private int consumerCount;

	public int getConsumerCount() {
		return consumerCount;
	}
}
