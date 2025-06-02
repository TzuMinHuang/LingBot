package idv.hzm.app.portal.service;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.common.util.CodeGeneratorUtil;
import idv.hzm.app.portal.dto.InitialClientInfo;
import idv.hzm.app.portal.entity.Session;
import idv.hzm.app.portal.repo.SessionsRepository;

@Service
public class ChatSessionService {

	@Autowired
	private CodeGeneratorUtil codeGeneratorUtil;
	@Autowired
	private SessionsRepository sessionRepository;
	@Autowired
	private ReplyUserQueueRegistrar replyUserQueueRegistrar;
	@Autowired
	private ChatRedisService chatRedisService;

	public InitialClientInfo initialClientInfo() {
		
		Session session =new Session() ;
		session.setId(this.codeGeneratorUtil.nextSessionId());
		session.setUserId(this.codeGeneratorUtil.nextUserCode());
		session.setStatus("active");
		session.setStartTime(Instant.now());
		session = this.sessionRepository.save(session);
		
		this.chatRedisService.saveChatSession(session);
		
		final String sessionId = session.getId().toString();
		
		this.replyUserQueueRegistrar.createReplyUserQueue(sessionId);
		
		InitialClientInfo initialClientInfo = new InitialClientInfo();
		initialClientInfo.setSessionId(sessionId);
		return initialClientInfo;
	}

}
