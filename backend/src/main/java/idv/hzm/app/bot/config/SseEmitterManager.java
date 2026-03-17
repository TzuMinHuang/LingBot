import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.dto.EventDto;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class SseEmitterManager {

	private static final Logger logger = LoggerFactory.getLogger(SseEmitterManager.class);

	private final ConcurrentHashMap<String, Sinks.Many<EventDto>> sinks = new ConcurrentHashMap<>();

	public Flux<EventDto> subscribe(String sessionId) {
		Sinks.Many<EventDto> sink = sinks.computeIfAbsent(sessionId, k -> 
			Sinks.many().multicast().directBestEffort()
		);
		
		return sink.asFlux()
				.doOnCancel(() -> {
					// Optional: Clean up sink if no more subscribers? 
					// For SSE, usually one subscriber per sessionId in this app.
					logger.info("[RE-SSE] Subscriber cancelled: sessionId={}", sessionId);
				});
	}

	public void send(String sessionId, EventDto event) {
		Sinks.Many<EventDto> sink = sinks.get(sessionId);
		if (sink != null) {
			sink.tryEmitNext(event);
		}
	}

	public boolean has(String sessionId) {
		return sinks.containsKey(sessionId);
	}

	public void remove(String sessionId) {
		Sinks.Many<EventDto> sink = sinks.remove(sessionId);
		if (sink != null) {
			sink.tryEmitComplete();
		}
	}

	public Set<String> getConnectedSessions() {
		return sinks.keySet();
	}
}
