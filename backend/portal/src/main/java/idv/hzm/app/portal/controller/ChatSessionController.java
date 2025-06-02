package idv.hzm.app.portal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import idv.hzm.app.portal.dto.InitialClientInfo;
import idv.hzm.app.portal.service.ChatSessionService;

@RestController
public class ChatSessionController {

	@Autowired
	private ChatSessionService chatSessionService;

	@PostMapping("/chat/initial")
	public InitialClientInfo initialClientInfo() {

		return this.chatSessionService.initialClientInfo();
	}
}
