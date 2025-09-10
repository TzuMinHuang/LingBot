package idv.hzm.app.bot.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

	@Bean
	public MessageConverter messageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	/*
	 * 回覆給使用者：：queue.reply.user.{userId}
	 */

	public static final String REPLY_USER_QUEUE_NAME = "queue.reply.user.%s";
	public static final String REPLY_USER_EXCHANGE_NAME = "exchange.reply.user";
	public static final String REPLY_USER_ROUTING_KEY_NAME = "reply.user.%s";

	@Bean
	public TopicExchange replyUserExchange() {
		return new TopicExchange(REPLY_USER_EXCHANGE_NAME);
	}

	/*
	 * 使用者訊息進入初始隊列： queue.incoming → bot 消費 → 意圖分析產生執行 JSON 後，傳入
	 * queue.execute_request
	 */
	public static final String INCOMING_USER_QUEUE_NAME = "queue.incoming.user";
	public static final String INCOMING_USER_EXCHANGE_NAME = "exchange.incoming.user";
	public static final String INCOMING_USER_ROUTING_KEY_NAME = "incoming.user";

	@Bean
	public Queue incomingUserQueue() {
		return new Queue(INCOMING_USER_QUEUE_NAME, true); // durable
	}

	@Bean
	public DirectExchange incomingUserExchange() {
		return new DirectExchange(INCOMING_USER_EXCHANGE_NAME);
	}

	@Bean
	public Binding incomingUserBinding(Queue incomingUserQueue, DirectExchange incomingUserExchange) {
		return BindingBuilder.bind(incomingUserQueue).to(incomingUserExchange).with(INCOMING_USER_ROUTING_KEY_NAME);
	}

	/*
	 * ExecuteConsumer： 根據 queue.execute_request 內的 Json 資料，執 action
	 */
	public static final String EXECUTE_REQUEST_QUEUE_NAME = "queue.execute_request";
	public static final String EXECUTE_REQUEST_EXCHANGE_NAME = "exchange.execute_request";
	public static final String EXECUTE_REQUEST_RROUTING_KEY_NAME = "execute_request";

	@Bean
	public Queue executeRequestQueue() {
		return new Queue(EXECUTE_REQUEST_QUEUE_NAME, true);
	}

	@Bean
	public DirectExchange executeRequestExchange() {
		return new DirectExchange(EXECUTE_REQUEST_EXCHANGE_NAME);
	}

	@Bean
	public Binding executeRequestBinding(Queue executeRequestQueue, DirectExchange executeRequestExchange) {
		return BindingBuilder.bind(executeRequestQueue).to(executeRequestExchange)
				.with(EXECUTE_REQUEST_RROUTING_KEY_NAME);
	}

	/*
	 * ExecuteConsumer： 根據 action 執行結果，放入 execute_respond queue 返回 bot， 在 LLM
	 * 根據結果回覆合適的回答
	 */
	public static final String EXECUTE_RESPOND_QUEUE_NAME = "queue.execute_respond";
	public static final String EXECUTE_RESPOND_EXCHANGE_NAME = "exchange.execute_respond";
	public static final String EXECUTE_RESPOND_RROUTING_KEY_NAME = "execute_respond";

	@Bean
	public Queue executeRespondQueue() {
		return new Queue(EXECUTE_RESPOND_QUEUE_NAME, true); // durable
	}

	@Bean
	public DirectExchange executeRespondExchange() {
		return new DirectExchange(EXECUTE_RESPOND_EXCHANGE_NAME);
	}

	@Bean
	public Binding executeRespondBinding(Queue executeRespondQueue, DirectExchange executeRespondExchange) {
		return BindingBuilder.bind(executeRespondQueue).to(executeRespondExchange)
				.with(EXECUTE_RESPOND_RROUTING_KEY_NAME);
	}

}
