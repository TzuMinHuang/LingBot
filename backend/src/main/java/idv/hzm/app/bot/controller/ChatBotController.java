package idv.hzm.app.bot.controller;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.config.SseSessionManager;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.InitialClientInfo;
import idv.hzm.app.bot.service.ChatHistoryService;
import idv.hzm.app.bot.service.ChatRedisService;
import idv.hzm.app.bot.service.ChatSessionService;
import idv.hzm.app.bot.service.SuggestionService;
import idv.hzm.app.bot.service.UiEventService;

@RestController
public class ChatBotController {

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);
	private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Z0-9]{12,20}$");

	@Value("${app.max-input-length:500}")
	private int maxInputLength;

	@Autowired
	private ChatSessionService chatSessionService;

	@Autowired
	private SseSessionManager sseSessionManager;

	@Autowired
	private SuggestionService suggestionService;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private ChatRedisService chatRedisService;

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private UiEventService uiEventService;

	@PostMapping("/chat/initial")
	public InitialClientInfo initialClientInfo(
			@RequestAttribute(name = "authenticatedUserId", required = false) String authenticatedUserId) {
		return this.chatSessionService.initialClientInfo(authenticatedUserId);
	}

	/**
	 * SSE 訂閱 — 前端連線後持續接收事件，支援 Last-Event-ID 斷線重連
	 */
	@GetMapping(value = "/chat/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<EventDto>> stream(
			@PathVariable String sessionId,
			@RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
		if (!isValidSessionId(sessionId)) {
			return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid sessionId"));
		}
		if (!chatRedisService.isSessionActive(sessionId)) {
			return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "session not found or expired"));
		}

		logger.info("[SSE] Client connected: sessionId={}, lastEventId={}", sessionId, lastEventId);

		// 訂閱新的 SseSessionManager（BlockingQueue 模型）
		Flux<EventDto> eventFlux = sseSessionManager.subscribe(sessionId);

		// 如果有 Last-Event-ID，恢復未送達的訊息
		if (lastEventId != null && !lastEventId.isBlank()) {
			// 格式 "messageId:seqIndex" → DB 恢復; 否則 → Redis Stream 恢復
			String[] parts = lastEventId.split(":", 2);
			if (parts.length == 2 && parts[1].matches("\\d+")) {
				sseSessionManager.recoverMissedChunksFromDb(sessionId, parts[0], Integer.parseInt(parts[1]));
			} else {
				sseSessionManager.recoverFromLastEventId(sessionId, lastEventId);
			}
		}

		return eventFlux
				.map(event -> ServerSentEvent.<EventDto>builder()
						.id(event.getInteractionId())
						.event("chat")
						.data(event)
						.build())
				.doOnTerminate(() -> logger.info("[SSE] Stream terminated: sessionId={}", sessionId));
	}

	/**
	 * 發送訊息 — 寫入 stream:request
	 */
	@PostMapping("/chat/{sessionId}/send")
	public Mono<ResponseEntity<Object>> send(
			@PathVariable String sessionId,
			@RequestBody Map<String, String> body) {
		if (!isValidSessionId(sessionId)) {
			return Mono.just(ResponseEntity.badRequest().body((Object) "invalid sessionId"));
		}
		if (!chatRedisService.isSessionActive(sessionId)) {
			return Mono.just(ResponseEntity.badRequest().body((Object) "session not found or expired"));
		}

		String content = body.get("content");
		if (content == null || content.isBlank()) {
			return Mono.just(ResponseEntity.badRequest().body((Object) "content is required"));
		}
		if (content.length() > maxInputLength) {
			logger.warn("Message from session {} exceeds max length ({}), rejected", sessionId, content.length());
			return Mono.just(ResponseEntity.badRequest().body((Object) "content exceeds max length"));
		}

		// 冪等性檢查：同一時間只能提問一次
		if (!chatRedisService.acquireRequestLock(sessionId)) {
			logger.warn("Concurrent message from session {}, rejected", sessionId);
			return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
		}

		try {
			// Track question frequency for suggestions
			suggestionService.recordQuestion(content);

			// Persist user message and get interactionId
			String interactionId = chatHistoryService.processIncomingMessage(sessionId, content);

			Map<String, String> record = Map.of(
					"sessionId", sessionId,
					"content", content,
					"interactionId", interactionId);

			return chatRedisService.checkIdempotency(interactionId)
					.flatMap(active -> {
						if (Boolean.TRUE.equals(active)) {
							return Mono.just(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build());
						}

						return reactiveRedisTemplate.opsForStream()
								.add(RedisStreamConfig.REQUEST_STREAM, record)
								.map(id -> ResponseEntity.ok((Object) Map.of("interactionId", interactionId)));
					});
		} catch (Exception e) {
			logger.error("Failed to process incoming message: {}", e.getMessage());
			chatRedisService.releaseRequestLock(sessionId);
			return Mono.just(ResponseEntity.internalServerError().body((Object) "failed to process message"));
		}
	}

	/**
	 * 手動取消目前的提問（釋放鎖）
	 */
	@PostMapping("/chat/{sessionId}/stop")
	public Mono<ResponseEntity<?>> stop(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return Mono.just(ResponseEntity.badRequest().body("invalid sessionId"));
		}
		chatRedisService.releaseRequestLock(sessionId);
		logger.info("[STOP] Released lock for sessionId: {}", sessionId);
		return Mono.just(ResponseEntity.ok().build());
	}

	/**
	 * 取得對話歷史紀錄
	 */
	@GetMapping("/chat/{sessionId}/history")
	public Mono<ResponseEntity<?>> history(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return Mono.just(ResponseEntity.badRequest().body("invalid sessionId"));
		}
		Map<String, Object> result = chatHistoryService.getSessionHistory(sessionId);
		if (result.get("userId") == null) {
			return Mono.just(ResponseEntity.notFound().build());
		}
		return Mono.just(ResponseEntity.ok((Object) result));
	}

	/**
	 * 查詢排隊位置 — 基於 stream:request 的 pending count
	 */
	@GetMapping("/chat/{sessionId}/queue-position")
	public Mono<ResponseEntity<Object>> queuePosition(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return Mono.just(ResponseEntity.badRequest().body((Object) "invalid sessionId"));
		}

		return reactiveRedisTemplate.execute(conn -> conn.streamCommands()
						.xInfoGroups(ByteBuffer.wrap(RedisStreamConfig.REQUEST_STREAM.getBytes())))
				.filter(group -> RedisStreamConfig.REQUEST_CONSUMER_GROUP.equals(group.groupName()))
				.next()
				.map(group -> {
					long pending = group.pendingCount() != null ? group.pendingCount() : 0;
					return ResponseEntity.ok((Object) Map.of("position", pending));
				})
				.defaultIfEmpty(ResponseEntity.ok((Object) Map.of("position", 0)))
				.onErrorResume(e -> {
					logger.warn("Failed to get queue position via XINFO: {}", e.getMessage());
					return Mono.just(ResponseEntity.ok((Object) Map.of("position", 0)));
				});
	}

	/**
	 * 接收前端 UI 事件（PAUSE, CANCEL, FAQ_CLICK 等）
	 */
	@PostMapping("/chat/{sessionId}/events")
	public ResponseEntity<?> postUiEvent(
			@PathVariable String sessionId,
			@RequestBody Map<String, String> body) {
		if (!isValidSessionId(sessionId)) {
			return ResponseEntity.badRequest().body("invalid sessionId");
		}

		String eventType = body.get("eventType");
		if (eventType == null || eventType.isBlank()) {
			return ResponseEntity.badRequest().body("eventType is required");
		}

		String payload = body.get("payload");
		uiEventService.recordEvent(sessionId, eventType, payload);

		// PAUSE/CANCEL 同時設定 Redis 控制鍵，供 RequestConsumer 檢查
		if ("PAUSE".equals(eventType) || "CANCEL".equals(eventType)) {
			chatRedisService.setControlSignal(sessionId, eventType);
		}

		return ResponseEntity.ok(Map.of("status", "recorded"));
	}

	/**
	 * 取得 session 的 UI 事件歷史（供前端重建 UI 狀態）
	 */
	@GetMapping("/chat/{sessionId}/events")
	public ResponseEntity<?> getUiEvents(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return ResponseEntity.badRequest().body("invalid sessionId");
		}
		return ResponseEntity.ok(uiEventService.getSessionEvents(sessionId));
	}

	private boolean isValidSessionId(String sessionId) {
		return sessionId != null && SESSION_ID_PATTERN.matcher(sessionId).matches();
	}

	/**
	 * 取得常見問題 — 依詢問頻率排序
	 */
	@GetMapping("/chat/suggestions")
	public Mono<Map<String, List<String>>> suggestions(@RequestParam(defaultValue = "10") int limit) {
		return Mono.fromCallable(() -> suggestionService.getTopSuggestions(limit))
				.map(items -> Map.of("suggestions", items));
	}
}
