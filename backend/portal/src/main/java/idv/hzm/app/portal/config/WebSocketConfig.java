package idv.hzm.app.portal.config;

import java.io.IOException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.common.dto.EventDto;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")// WebSocket 入口
				.setAllowedOriginPatterns("*").withSockJS();// 或使用原生 WebSocket
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic"); // 前端訂閱用,推播端點
		registry.setApplicationDestinationPrefixes("/app"); // 前端傳送用端點
	}

	public static final String WEBSOCKET_CHANNEL_NAME = "ws-channel";

	@Bean
	public RedisMessageListenerContainer redisContainer(RedisConnectionFactory factory,
			MessageListenerAdapter adapter) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(adapter, new ChannelTopic(WEBSOCKET_CHANNEL_NAME));
		return container;
	}

	// @SendTo("/topic/user/${userId}/receive")

	@Bean
	public MessageListener webSocketSubscriber(SimpMessagingTemplate messagingTemplate) {
		return (Message message, byte[] pattern) -> {
			ObjectMapper mapper = new ObjectMapper();
			try {
				EventDto chatMsg = mapper.readValue(message.getBody(), EventDto.class);
				messagingTemplate.convertAndSend("/topic/user/" + chatMsg.getSessionId() + "/receive", chatMsg);
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
	}

	@Bean
	public MessageListenerAdapter listenerAdapter(MessageListener webSocketSubscriber) {
		return new MessageListenerAdapter(webSocketSubscriber, "onMessage");
	}

}
