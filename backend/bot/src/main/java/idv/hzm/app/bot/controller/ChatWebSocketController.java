package idv.hzm.app.bot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import idv.hzm.app.bot.service.RabbitMQService;
import idv.hzm.app.common.dto.MessagePayload;

@Controller
public class ChatWebSocketController {

	private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketController.class);

	@Autowired
	private RabbitMQService rabbitMQService;

	@MessageMapping("/chat/{sessionId}/send")
	public void incomingUser(@DestinationVariable String sessionId, @Payload MessagePayload basePayload) {

		try {
			this.rabbitMQService.sendToIncomingUserQueue(sessionId, basePayload.getContent());
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
	}

}
