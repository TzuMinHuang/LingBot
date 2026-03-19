package idv.hzm.app.bot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import idv.hzm.app.bot.config.SseSessionManager;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.MessagePayload;
import idv.hzm.app.bot.entity.MessageChunk;
import idv.hzm.app.bot.repo.MessageChunkRepository;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the SseSessionManager's subscribe/enqueue contract and
 * Last-Event-ID historical DB chunk recovery sequence using StepVerifier.
 */
@Tag("slow")
class SseSessionManagerTest extends AbstractIntegrationTest {

	@Autowired
	private SseSessionManager sseSessionManager;

	@Autowired
	private MessageChunkRepository messageChunkRepository;

	private static final String[] TEST_SESSION_IDS = { "ORDERSESSION01", "RECOVERSESS01", "LOCALSESS0001" };

	@AfterEach
	void cleanupSessions() {
		for (String sessionId : TEST_SESSION_IDS) {
			sseSessionManager.remove(sessionId);
		}
	}

	@Test
	void subscribe_emitsEnqueuedEventsInOrder() {
		String sessionId = "ORDERSESSION01";

		var flux = sseSessionManager.subscribe(sessionId);

		// Enqueue events
		for (int i = 1; i <= 5; i++) {
			EventDto event = new EventDto();
			event.setSessionId(sessionId);
			event.setInteractionId("order-int-" + i);
			event.setType(EventDto.TYPE_STREAM_CHUNK);
			event.setPayload(new MessagePayload("chunk-" + i));
			sseSessionManager.enqueue(sessionId, event);
		}

		// Enqueue a terminal event to end the stream
		EventDto end = new EventDto();
		end.setSessionId(sessionId);
		end.setInteractionId("order-int-end");
		end.setType(EventDto.TYPE_STREAM_END);
		end.setPayload(new MessagePayload());
		sseSessionManager.enqueue(sessionId, end);

		// Verify events arrive in order
		List<String> received = new ArrayList<>();

		StepVerifier.create(flux.take(6).timeout(Duration.ofSeconds(5)))
				.thenConsumeWhile(event -> {
					if (event.getPayload() instanceof MessagePayload mp) {
						received.add(mp.getContent());
					}
					return true;
				})
				.verifyComplete();

		assertThat(received).containsExactly("chunk-1", "chunk-2", "chunk-3", "chunk-4", "chunk-5", "");
	}

	@Test
	void recoverMissedChunksFromDb_emitsHistoricalChunksBeforeLiveEvents() {
		String sessionId = "RECOVERSESS01";
		String messageId = "msg-recover-001";
		String interactionId = "int-recover-001";

		// Seed database with historical chunks (seqIndex 0..4)
		for (int i = 0; i < 5; i++) {
			MessageChunk chunk = new MessageChunk();
			chunk.setMessageId(messageId);
			chunk.setSessionId(sessionId);
			chunk.setInteractionId(interactionId);
			chunk.setSeqIndex(i);
			chunk.setContent("db-chunk-" + i);
			chunk.setCreatedAt(Instant.now());
			messageChunkRepository.save(chunk);
		}

		// Subscribe first (creates queue + sink)
		var flux = sseSessionManager.subscribe(sessionId);

		// Recover chunks after seqIndex 1 (should emit chunks 2, 3, 4)
		sseSessionManager.recoverMissedChunksFromDb(sessionId, messageId, 1);

		// Then enqueue a live event
		EventDto liveEvent = new EventDto();
		liveEvent.setSessionId(sessionId);
		liveEvent.setInteractionId(interactionId);
		liveEvent.setType(EventDto.TYPE_STREAM_CHUNK);
		liveEvent.setPayload(new MessagePayload("live-chunk"));
		sseSessionManager.enqueue(sessionId, liveEvent);

		// Verify: historical chunks come BEFORE the live event
		List<String> contents = new ArrayList<>();

		StepVerifier.create(flux.take(4).timeout(Duration.ofSeconds(5)))
				.thenConsumeWhile(event -> {
					if (event.getPayload() instanceof MessagePayload mp) {
						contents.add(mp.getContent());
					}
					return true;
				})
				.verifyComplete();

		// Historical: db-chunk-2, db-chunk-3, db-chunk-4, then live-chunk
		assertThat(contents).containsExactly("db-chunk-2", "db-chunk-3", "db-chunk-4", "live-chunk");
	}

	@Test
	void isLocalSession_returnsTrueAfterSubscribe() {
		String sessionId = "LOCALSESS0001";

		assertThat(sseSessionManager.isLocalSession(sessionId)).isFalse();

		sseSessionManager.subscribe(sessionId);

		assertThat(sseSessionManager.isLocalSession(sessionId)).isTrue();

		sseSessionManager.remove(sessionId);

		assertThat(sseSessionManager.isLocalSession(sessionId)).isFalse();
	}
}
