package idv.hzm.app.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 訊息流程：
// 前端 POST /chat/{sessionId}/send → ChatBotController → stream:request
// → RequestConsumer → AnythingLLM API → LlmThrottlingBuffer → stream:response
// → ResponseConsumer → SseSessionManager (BlockingQueue) → SSE /chat/{sessionId}/stream → 前端

@SpringBootApplication
@EnableScheduling
public class BotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BotApplication.class, args);
	}
}
