package idv.hzm.app.bot.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import idv.hzm.app.common.domain.RabbitMQProperties;

@Configuration
public class RabbitMQConfig {

	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	/*
	 * 使用者訊息進入初始隊列： queue.incoming → bot 消費 → 意圖分析後分流
	 */

	@Bean
	public Queue incomingUserQueue() {
		return new Queue(RabbitMQProperties.INCOMING_USER_QUEUE_NAME, true); // durable
	}

	@Bean
	public DirectExchange incomingUserExchange() {
		return new DirectExchange(RabbitMQProperties.INCOMING_USER_EXCHANGE_NAME);
	}

	@Bean
	public Binding incomingUserBinding(Queue incomingUserQueue, DirectExchange incomingUserExchange) {
		return BindingBuilder.bind(incomingUserQueue).to(incomingUserExchange)
				.with(RabbitMQProperties.INCOMING_USER_ROUTING_KEY_NAME);
	}

}
