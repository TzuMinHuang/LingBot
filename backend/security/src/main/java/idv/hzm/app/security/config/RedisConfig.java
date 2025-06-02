package idv.hzm.app.security.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import idv.hzm.app.common.config.BaseRedisConfig;

/**
 * Redis配置類
 */
@EnableCaching
@Configuration
public class RedisConfig extends BaseRedisConfig {}
