package idv.hzm.app.admin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
	    template.setConnectionFactory(factory);

	    // 設定 key 與 hashKey 都使用 String 序列化器
	    template.setKeySerializer(new StringRedisSerializer());
	    template.setHashKeySerializer(new StringRedisSerializer());

	    // 設定 value 可用 JSON 格式儲存（可選）
	    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
	    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

	    template.afterPropertiesSet();
		
		return template;
	}
}
