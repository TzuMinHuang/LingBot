package idv.hzm.app.portal.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.common.domain.RabbitMQProperties;
import idv.hzm.app.portal.listener.ReplyUserConsumer;

@Service
public class ReplyUserQueueRegistrar {

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
		String replyUserQueueName = String.format(RabbitMQProperties.REPLY_USER_QUEUE_NAME, userId);

		Map<String, Object> args = new HashMap<>();
		args.put("x-message-ttl", 60 * 1000); // 1 分鐘
		args.put("x-expires", 15 * 60 * 1000); // queue 空閒 15 分鐘自動刪除
		args.put("x-auto-delete", true); // 無人消費自動刪除

		Queue replyUserQueue = new Queue(replyUserQueueName, true, false, false, args);
		this.rabbitAdmin.declareQueue(replyUserQueue);

		// 2. 綁定 Exchange（topic）
		String replyUserRoutingKey = String.format(RabbitMQProperties.REPLY_USER_ROUTING_KEY_NAME, userId);

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
		String replyUserQueueName = String.format(RabbitMQProperties.REPLY_USER_QUEUE_NAME, userId);
		this.rabbitAdmin.deleteQueue(replyUserQueueName);
	}

	/**
	 * 停止 userId 的 ReplyUserQueue 消費者監聽
	 */
	public void stopAndRemoveListening(String userId) {
		Optional.ofNullable(this.replyUserQueueListenerContainers.remove(userId))
				.ifPresent(SimpleMessageListenerContainer::stop);
	}
}
