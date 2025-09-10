package idv.hzm.app.admin.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	/*
	 * 客服接線流程： 客服點擊接線 → queue.customer_service.{roomId}
	 * 雙向對話：queue.customer_service.{roomId} <→ queue.reply.user.{userId}
	 */
	
	public final static String CUSTOMER_SERVICE_EXCHANGE_NAME = "exchange.customer_service.%s";

	@Bean
	public TopicExchange customerServiceExchange() {
		return new TopicExchange(CUSTOMER_SERVICE_EXCHANGE_NAME);
	}

	/*
	 * Agent 接收消息的 Queue ： ：queue.receive.agent.{agentId}
	 */
	
	public final static String RECEIVE_AGENT_EXCHANGE_NAME = "exchange.receive.agent.%s";

	@Bean
	public TopicExchange receiveAgentExchange() {
		return new TopicExchange(RECEIVE_AGENT_EXCHANGE_NAME);
	}

}
