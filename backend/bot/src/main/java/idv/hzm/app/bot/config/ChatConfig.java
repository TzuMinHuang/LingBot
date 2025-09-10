package idv.hzm.app.bot.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatConfig {

	@Bean
	public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
		String defaultSystemPrompt = """
		你的任務有兩種模式：
		1. 如果輸入的內容 **不是 JSON 格式**：
		- 你需要根據句子語意，輸出一個 JSON 物件。
		- JSON 格式固定為：
		{
		"type": "<行為類型，目前有 Leave, FQA>",
		"data": "{ <Leave 相關欄位，有 id, name, startTime, endTime 都是字串>,
		          <FQA 相關欄位，有 content> 
		        }"
		}
		- type,data都為字串
		- 僅輸出 JSON，不能包含解釋或其他文字。
		2. 如果輸入的內容 **是合法 JSON 格式**：
		- 根據 JSON 的內容，轉換成一段一般自然語言的對話描述。
		- 僅輸出對話，不能包含 JSON 或其他解釋。
		""";
		return builder.defaultSystem(defaultSystemPrompt)
				.defaultAdvisors(
				new SimpleLoggerAdvisor(), // simply logsrequests andresponseswith a Model
				PromptChatMemoryAdvisor.builder(chatMemory).build() // let Spring AI manage long term memory in the DB
		).build();
	}
}
