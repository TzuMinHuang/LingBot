package idv.hzm.app.bot.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ThreadPoolConfig {

	@Value("${app.sse-pool-size:50}")
	private int ssePoolSize;

	private ExecutorService sseExecutor;
	private ScheduledExecutorService throttleScheduler;

	@Bean(name = "sseExecutor")
	public ExecutorService sseExecutor() {
		sseExecutor = Executors.newFixedThreadPool(ssePoolSize, r -> {
			Thread t = new Thread(r);
			t.setName("sse-sender-" + t.getId());
			t.setDaemon(true);
			return t;
		});
		return sseExecutor;
	}

	@Bean(name = "throttleScheduler")
	public ScheduledExecutorService throttleScheduler() {
		throttleScheduler = Executors.newScheduledThreadPool(2, r -> {
			Thread t = new Thread(r);
			t.setName("throttle-timer-" + t.getId());
			t.setDaemon(true);
			return t;
		});
		return throttleScheduler;
	}

	@PreDestroy
	public void shutdown() {
		if (sseExecutor != null) {
			sseExecutor.shutdown();
		}
		if (throttleScheduler != null) {
			throttleScheduler.shutdown();
		}
	}
}
