package idv.hzm.app.portal.listener;

import java.nio.charset.StandardCharsets;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import idv.hzm.app.common.dto.EventDto;
import idv.hzm.app.common.util.Json2Util;
import idv.hzm.app.portal.service.WebSocketPublisher;

@Component
public class ReplyUserConsumer implements MessageListener {

	@Autowired
	private WebSocketPublisher webSocketPublisher;

	@Override
	public void onMessage(Message message) {
		String payload = new String(message.getBody(), StandardCharsets.UTF_8);
		EventDto chatMessage = Json2Util.fromJson(payload, EventDto.class);
		this.webSocketPublisher.publish(chatMessage);
	}
}