package idv.hzm.app.bot;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// BotConsumer 將使用者的 message 透過 LLM 轉成 json 放入 excuet queue
// excuet Consumer 消費 excuet queue 根據 JSON date 驗證資料,呼叫 API
// excuet Consumer 將呼叫 API 的結果 傳入 replay bot queue
// BotConsumer 將replay bot queue 的資料送入 LLM 產生回話 傳入 replayuser queue 返回 user

@SpringBootApplication()
public class BotApplication {

	public static void main(String[] args) {
		SpringApplication.run(BotApplication.class, args);
	}
	@Bean
	public CommandLineRunner runner() {
	    return args -> {
	
	    };
	}

}
