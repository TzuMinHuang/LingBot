package idv.hzm.app.portal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import idv.hzm.app.common.dto.BasePayload;
import idv.hzm.app.portal.service.MessageTransferService;

@Controller
public class ChatWebSocketController {

	@Autowired
	private MessageTransferService messageTransferService;

	@MessageMapping("/chat/{sessionId}/send")
	public void incomingUser(@DestinationVariable String sessionId, @Payload BasePayload basePayload) {

		this.messageTransferService.messageTransfer(sessionId, basePayload);
	}

}
