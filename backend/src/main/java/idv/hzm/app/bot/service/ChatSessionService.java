package idv.hzm.app.bot.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.dto.InitialClientInfo;
import idv.hzm.app.bot.entity.Session;
import idv.hzm.app.bot.entity.SessionStatus;
import idv.hzm.app.bot.repo.SessionsRepository;
import idv.hzm.app.bot.utils.CodeGeneratorUtil;

@Service
public class ChatSessionService {

	@Autowired
	private CodeGeneratorUtil codeGeneratorUtil;
	@Autowired
	private SessionsRepository sessionRepository;
	@Autowired
	private ChatRedisService chatRedisService;

	public InitialClientInfo initialClientInfo() {

		Session session = new Session();
		session.setId(this.codeGeneratorUtil.nextSessionId());
		session.setUserId(this.codeGeneratorUtil.nextUserCode());
		Instant now = Instant.now();
		session.setStatus(SessionStatus.ACTIVE);
		session.setStartTime(now);
		session.setExpiresAt(now.plusSeconds(86400)); // 24h TTL

		this.sessionRepository.save(session);
		this.chatRedisService.saveChatSession(session);

		final String sessionId = session.getId().toString();

		InitialClientInfo initialClientInfo = new InitialClientInfo();
		initialClientInfo.setSessionId(sessionId);
		return initialClientInfo;
	}

}
