package idv.hzm.app.bot.config;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import idv.hzm.app.bot.dto.EventDto;

@Component
public class SseEmitterManager {

	private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);

	@Value("${app.sse-timeout-minutes:5}")
	private int sseTimeoutMinutes;

	private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	public SseEmitter create(String sessionId) {
		// 若已存在舊 emitter，先完成它
		SseEmitter old = emitters.remove(sessionId);
		if (old != null) {
			try {
				old.complete();
			} catch (Exception e) {
				logger.debug("[SSE] Error completing old emitter: sessionId={}", sessionId);
			}
		}

		SseEmitter emitter = new SseEmitter(sseTimeoutMinutes * 60 * 1000L);

		emitter.onCompletion(() -> {
			emitters.remove(sessionId, emitter);
			logger.info("[SSE] Emitter completed: sessionId={}", sessionId);
		});
		emitter.onTimeout(() -> {
			emitters.remove(sessionId, emitter);
			logger.info("[SSE] Emitter timed out: sessionId={}", sessionId);
		});
		emitter.onError(e -> {
			emitters.remove(sessionId, emitter);
			logger.debug("[SSE] Emitter error: sessionId={}, error={}", sessionId, e.getMessage());
		});

		emitters.put(sessionId, emitter);
		logger.info("[SSE] Emitter created: sessionId={}", sessionId);
		return emitter;
	}

	public void send(String sessionId, EventDto event) {
		SseEmitter emitter = emitters.get(sessionId);
		if (emitter == null)
			return;

		try {
			emitter.send(SseEmitter.event().name("chat").data(event));
		} catch (IOException e) {
			logger.debug("[SSE] Send failed, removing emitter: sessionId={}", sessionId);
			emitters.remove(sessionId, emitter);
		}
	}

	public boolean has(String sessionId) {
		return emitters.containsKey(sessionId);
	}

	public void remove(String sessionId) {
		SseEmitter emitter = emitters.remove(sessionId);
		if (emitter != null) {
			emitter.complete();
		}
	}

	public Set<String> getConnectedSessions() {
		return emitters.keySet();
	}
}
