package idv.hzm.app.bot.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import idv.hzm.app.bot.entity.UiEvent;
import idv.hzm.app.bot.repo.UiEventRepository;

@Service
public class UiEventService {

	private static final Logger logger = LoggerFactory.getLogger(UiEventService.class);

	@Autowired
	private UiEventRepository uiEventRepository;

	@Transactional
	public UiEvent recordEvent(String sessionId, String eventType, String payload) {
		int nextSeq = uiEventRepository.findMaxSeqIndex(sessionId) + 1;

		UiEvent event = new UiEvent();
		event.setSessionId(sessionId);
		event.setSeqIndex(nextSeq);
		event.setEventType(eventType);
		event.setPayload(payload);
		event.setCreatedAt(Instant.now());

		uiEventRepository.save(event);
		logger.debug("[UI-EVENT] Recorded event: sessionId={}, type={}, seq={}", sessionId, eventType, nextSeq);
		return event;
	}

	public List<UiEvent> getSessionEvents(String sessionId) {
		return uiEventRepository.findBySessionIdOrderBySeqIndexAsc(sessionId);
	}
}
