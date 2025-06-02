package idv.hzm.app.portal.service;

import java.util.Map;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.common.domain.RabbitMQProperties;
import idv.hzm.app.common.dto.BasePayload;
import idv.hzm.app.common.dto.EventDto;
import idv.hzm.app.common.dto.EventFactory;
import idv.hzm.app.common.util.Json2Util;
import idv.hzm.app.portal.config.RabbitMQConfig;
import idv.hzm.app.portal.entity.Session;

@Service
public class MessageTransferService {

	@Autowired
	private AmqpTemplate rabbitTemplate;

	@Autowired
	private ChatRedisService chatRedisService;

	public void messageTransfer(String sessionId, BasePayload basePayload) {

		try {
			EventDto eventDto = EventFactory.createEvent(sessionId, basePayload);
			final String message = Json2Util.toJson(eventDto);
			System.out.println(message);
			Session chatSession = this.chatRedisService.getChatSession(sessionId);
			final String agentId = chatSession.getAgentId();
			if (agentId == null) {
				this.transferToBot(message);
			} else {
				this.transferToCustomerService(agentId, message);
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

	}

	/**
	 * 將訊息傳遞到 queue.incoming.user， bot 會收到訊息，根據意圖執行流程
	 * 
	 * @param chatMessage
	 */
	private void transferToBot(String message) {

		this.rabbitTemplate.convertAndSend(RabbitMQProperties.INCOMING_USER_EXCHANGE_NAME,
				RabbitMQProperties.INCOMING_USER_ROUTING_KEY_NAME, message);
	}

	/**
	 * 將訊息傳遞到 queue.customer_service， anget 會收到訊息，anget 會將資訊傳遞到 queue.reply.user
	 * 而形成雙向通知
	 * 
	 * @param chatMessage
	 */
	private void transferToCustomerService(String agentId, String message) {

		String routingKey = String.format(RabbitMQProperties.CUSTOMER_SERVICE_RROUTING_KEY_NAME, agentId);
		this.rabbitTemplate.convertAndSend(RabbitMQProperties.CUSTOMER_SERVICE_EXCHANGE_NAME, routingKey, message);
	}
}
