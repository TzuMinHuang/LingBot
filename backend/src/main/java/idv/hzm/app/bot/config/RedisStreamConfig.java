package idv.hzm.app.bot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisStreamConfig {

	public static final String REQUEST_STREAM = "stream:request";
	public static final String RESPONSE_STREAM = "stream:response";
	public static final String REQUEST_CONSUMER_GROUP = "request-consumer-group";
	public static final long STREAM_MAXLEN = 1000;

	public static final String BOT_DLQ_STREAM = "stream:bot:dlq";

	@Value("${app.consumer-count:8}")
	private int consumerCount;

	public int getConsumerCount() {
		return consumerCount;
	}
}
