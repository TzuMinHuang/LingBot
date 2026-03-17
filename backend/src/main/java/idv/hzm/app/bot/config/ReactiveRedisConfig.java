package idv.hzm.app.bot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamReceiver;

import java.time.Duration;

@Configuration
public class ReactiveRedisConfig {

    @Bean
    @Primary
    public ReactiveRedisTemplate<String, String> reactiveStringRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<>(factory, RedisSerializationContext.string());
    }

    @Bean
    public StreamReceiver<String, ?> streamReceiver(ReactiveRedisConnectionFactory factory) {
        StreamReceiver.StreamReceiverOptions<String, ?> options = StreamReceiver.StreamReceiverOptions.builder()
                .pollTimeout(Duration.ofMillis(100))
                .build();
        return StreamReceiver.create(factory, options);
    }
}
