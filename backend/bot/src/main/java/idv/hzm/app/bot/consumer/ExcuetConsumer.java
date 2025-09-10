package idv.hzm.app.bot.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.config.RabbitMQConfig;
import idv.hzm.app.bot.service.ActionService;
import idv.hzm.app.bot.service.RabbitMQService;

import idv.hzm.app.bot.dto.BasePayload;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;

@Component
public class ExcuetConsumer {

	private static final Logger logger = LoggerFactory.getLogger(ExcuetConsumer.class);

	@Autowired
	private ActionService actionService;

	@Autowired
	private RabbitMQService rabbitMQService;

	@RabbitListener(queues = RabbitMQConfig.EXECUTE_REQUEST_QUEUE_NAME)
	public void receiveBotMessage(EventDto eventDto) {
		final String sessionId = eventDto.getSessionId();
		String message = "";
		try {
			BasePayload payload = eventDto.getPayload();
			if (payload instanceof MessagePayload) {
				MessagePayload msg = (MessagePayload) payload;
				final String content = msg.getContent();
				message = this.actionService.handle(content);
			}
		} catch (Exception e) {
			logger.error(e.getMessage());
			message = e.getMessage();
		} finally {
			this.rabbitMQService.sendToExecuteRespondQueue(sessionId, message);
		}
	}

}
