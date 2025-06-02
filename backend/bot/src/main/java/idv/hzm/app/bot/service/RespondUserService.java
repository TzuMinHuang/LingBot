package idv.hzm.app.bot.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.entity.RobIntent;
import idv.hzm.app.bot.repo.RobIntentRepository;
import idv.hzm.app.common.domain.RabbitMQProperties;
import idv.hzm.app.common.dto.EventDto;
import idv.hzm.app.common.dto.EventFactory;
import idv.hzm.app.common.dto.MessagePayload;
import idv.hzm.app.common.util.Json2Util;

@Service
public class RespondUserService {

	@Autowired
	private RobIntentRepository robIntentRepository;
	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void respondToUserByIntent(String sessionId, String intentName) {

		RobIntent robIntent = this.robIntentRepository.findByName(intentName).orElseGet(() -> {
			RobIntent r = new RobIntent();
			r.setResponseText("需要真人客服嗎");
			return r;
		});

		this.respondToUserWithText(sessionId, robIntent.getResponseText());
	}

	public void respondToUserWithText(String sessionId, String responseText) {

		MessagePayload newMessagePayload = new MessagePayload();
		newMessagePayload.setContent(responseText);

		EventDto newEventDto = EventFactory.createEvent(sessionId, newMessagePayload);
		System.out.println(newEventDto);
		String routingKey = String.format(RabbitMQProperties.REPLY_USER_ROUTING_KEY_NAME, newEventDto.getSessionId());
		this.rabbitTemplate.convertAndSend(RabbitMQProperties.REPLY_USER_EXCHANGE_NAME, routingKey,
				Json2Util.toJson(newEventDto));
	}

}
