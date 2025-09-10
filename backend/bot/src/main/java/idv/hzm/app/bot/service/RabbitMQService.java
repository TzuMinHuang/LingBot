package idv.hzm.app.bot.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.config.RabbitMQConfig;
import idv.hzm.app.bot.consumer.ReplyUserConsumer;

import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.EventFactory;
import idv.hzm.app.bot.dto.MessagePayload;
import idv.hzm.app.bot.utils.Json2Util;

@Service
public class RabbitMQService {

	private static final Logger logger = LoggerFactory.getLogger(RabbitMQService.class);

	@Autowired
	private RabbitAdmin rabbitAdmin;

	@Autowired
	private TopicExchange replyUserExchange;

	@Autowired
	private ReplyUserConsumer replyUserConsumer;

	private final Map<String, SimpleMessageListenerContainer> replyUserQueueListenerContainers = new ConcurrentHashMap<>();

	/**
	 * 根據 userId 建立 ReplyUserQueue
	 */
	public void createReplyUserQueue(String userId) {
		if (this.replyUserQueueListenerContainers.containsKey(userId))
			return;

		// 1. 建立 Queue（含 TTL）
		String replyUserQueueName = String.format(RabbitMQConfig.REPLY_USER_QUEUE_NAME, userId);

		Map<String, Object> args = new HashMap<>();
		args.put("x-message-ttl", 60 * 1000); // 1 分鐘
		args.put("x-expires", 15 * 60 * 1000); // queue 空閒 15 分鐘自動刪除
		args.put("x-auto-delete", true); // 無人消費自動刪除

		Queue replyUserQueue = new Queue(replyUserQueueName, true, false, false, args);
		this.rabbitAdmin.declareQueue(replyUserQueue);

		// 2. 綁定 Exchange（topic）
		String replyUserRoutingKey = String.format(RabbitMQConfig.REPLY_USER_ROUTING_KEY_NAME, userId);

		Binding binding = BindingBuilder.bind(replyUserQueue).to(this.replyUserExchange).with(replyUserRoutingKey);
		this.rabbitAdmin.declareBinding(binding);

		// 3. 建立 Listener
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(
				this.rabbitAdmin.getRabbitTemplate().getConnectionFactory());
		container.setQueueNames(replyUserQueueName);
		container.setMessageListener(this.replyUserConsumer);
		container.start();

		this.replyUserQueueListenerContainers.put(userId, container);
	}

	/**
	 * 刪除 userId 的 ReplyUserQueue
	 */
	public void deleteReplyUserQueue(String userId) {
		String replyUserQueueName = String.format(RabbitMQConfig.REPLY_USER_QUEUE_NAME, userId);
		this.rabbitAdmin.deleteQueue(replyUserQueueName);
	}

	/**
	 * 停止 userId 的 ReplyUserQueue 消費者監聽
	 */
	public void stopAndRemoveListening(String userId) {
		Optional.ofNullable(this.replyUserQueueListenerContainers.remove(userId))
				.ifPresent(SimpleMessageListenerContainer::stop);
	}

	@Autowired
	private AmqpTemplate rabbitTemplate;

	public void sendToReplyUserQueue(String sessionId, String content) {

		String routingKey = String.format(RabbitMQConfig.REPLY_USER_ROUTING_KEY_NAME, sessionId);
		this.rabbitTemplate.convertAndSend(RabbitMQConfig.REPLY_USER_EXCHANGE_NAME, routingKey, this.getTransmissionMessage(sessionId, content));
	}

	/**
	 * 將訊息傳遞到 queue.incoming.user
	 * 
	 * @param chatMessage
	 */
	public void sendToIncomingUserQueue(String sessionId, String content) {

		this.rabbitTemplate.convertAndSend(RabbitMQConfig.INCOMING_USER_EXCHANGE_NAME,
				RabbitMQConfig.INCOMING_USER_ROUTING_KEY_NAME, this.getTransmissionMessage(sessionId, content));
	}

	/**
	 * 將訊息傳遞到 queue.execute_request
	 * 
	 * @param chatMessage
	 */
	public void sendToExecuteRequestQueue(String sessionId, String content) {

		this.rabbitTemplate.convertAndSend(RabbitMQConfig.EXECUTE_REQUEST_EXCHANGE_NAME,
				RabbitMQConfig.EXECUTE_REQUEST_RROUTING_KEY_NAME, this.getTransmissionMessage(sessionId, content));
	}

	/**
	 * 將訊息傳遞到 queue.execute_respond
	 * 
	 * @param chatMessage
	 */
	public void sendToExecuteRespondQueue(String sessionId, String content) {

		this.rabbitTemplate.convertAndSend(RabbitMQConfig.EXECUTE_RESPOND_EXCHANGE_NAME,
				RabbitMQConfig.EXECUTE_RESPOND_RROUTING_KEY_NAME, this.getTransmissionMessage(sessionId, content));
	}

	private EventDto getTransmissionMessage(String sessionId, String content) {

		MessagePayload messagePayload = new MessagePayload();
		messagePayload.setContent(content);

		EventDto eventDto = EventFactory.createEvent(sessionId, messagePayload);
		logger.info(eventDto.toString());
		//final String message = Json2Util.toJson(eventDto);
		//logger.info(message);
		return eventDto;
	}

}
