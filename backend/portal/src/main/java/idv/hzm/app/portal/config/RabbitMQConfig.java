package idv.hzm.app.portal.config;

import org.springframework.amqp.core.TopicExchange;
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
	 * 回覆給使用者： 可處理：queue.reply.user.{userId}
	 */

	@Bean
	public TopicExchange replyUserExchange() {
		return new TopicExchange(RabbitMQProperties.REPLY_USER_EXCHANGE_NAME);
	}

}
