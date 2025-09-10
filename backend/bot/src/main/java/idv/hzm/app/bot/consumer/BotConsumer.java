package idv.hzm.app.bot.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.config.RabbitMQConfig;
import idv.hzm.app.bot.service.OllamaService;
import idv.hzm.app.bot.service.RabbitMQService;

import idv.hzm.app.bot.dto.BasePayload;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;

// BotConsumer 將使用者的 message 透過 LLM 轉成 json 放入 excuet queue
// excuet Consumer  消費 excuet queue 根據 JSON date 驗證資料,呼叫 API
// excuet Consumer  將呼叫 API 的結果 傳入 replay bot queue 
// BotConsumer  將replay bot queue 的資料送入  LLM 產生回話 傳入 replayuser queue 返回 user
@Component
public class BotConsumer {

	private static final Logger logger = LoggerFactory.getLogger(BotConsumer.class);

	@Autowired
	private ChatClient chatClient;
	@Autowired
	private RabbitMQService rabbitMQService;

	@RabbitListener(queues = RabbitMQConfig.INCOMING_USER_QUEUE_NAME)
	public void receiveUserMessage(EventDto eventDto) {
		final String sessionId = eventDto.getSessionId();
		try {
			BasePayload payload = eventDto.getPayload();
			if (payload instanceof MessagePayload) {
				MessagePayload msg = (MessagePayload) payload;
				final String content = msg.getContent();
				logger.debug("userContent : {}",content);
				String actionContent = this.chatClient.prompt().user(content)
						.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId)).call().content();
				logger.debug("actionContent : {}",actionContent);
				this.rabbitMQService.sendToExecuteRequestQueue(sessionId, actionContent);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

	@RabbitListener(queues = RabbitMQConfig.EXECUTE_RESPOND_QUEUE_NAME)
	public void replyUserMessage(EventDto eventDto) {
		final String sessionId = eventDto.getSessionId();
		try {
			BasePayload payload = eventDto.getPayload();
			if (payload instanceof MessagePayload) {
				MessagePayload msg = (MessagePayload) payload;
				final String content = msg.getContent();
				logger.debug("replyContent : {}",content);
				String replyUserContent = this.chatClient.prompt().user(content)
						.advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId)).call().content();
				logger.debug("replyUser : {}",replyUserContent);
				this.rabbitMQService.sendToReplyUserQueue(sessionId, replyUserContent);
			}

		} catch (Exception e) {
			logger.error(e.getMessage());
		}

	}

}
