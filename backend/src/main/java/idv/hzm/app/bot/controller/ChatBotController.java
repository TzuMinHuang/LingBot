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
import idv.hzm.app.bot.config.SseEmitterManager;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.InitialClientInfo;
import idv.hzm.app.bot.dto.QueuePayload;
import idv.hzm.app.bot.service.ChatHistoryService;
import idv.hzm.app.bot.service.ChatRedisService;
import idv.hzm.app.bot.service.ChatSessionService;
import idv.hzm.app.bot.service.SuggestionService;

@RestController
public class ChatBotController {

	private static final Logger logger = LoggerFactory.getLogger(ChatBotController.class);
	private static final Pattern SESSION_ID_PATTERN = Pattern.compile("^[A-Z0-9]{12,20}$");

	@Value("${app.max-input-length:500}")
	private int maxInputLength;

	@Autowired
	private ChatSessionService chatSessionService;

	@Autowired
	private SseEmitterManager sseEmitterManager;

	@Autowired
	private SuggestionService suggestionService;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private ChatRedisService chatRedisService;

	@Autowired
	private RedisStreamConfig redisStreamConfig;

	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@PostMapping("/chat/initial")
	public InitialClientInfo initialClientInfo() {
		return this.chatSessionService.initialClientInfo();
	}

	/**
	 * SSE 訂閱 — 前端連線後持續接收事件
	 */
	@GetMapping(value = "/chat/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<EventDto>> stream(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid sessionId"));
		}
		if (!chatRedisService.isSessionActive(sessionId)) {
			return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "session not found or expired"));
		}
		
		logger.info("[RE-SSE] Client connected: sessionId={}", sessionId);

		// Mode B: 從 Redis Stream 讀取歷史 (尚待實作：取得 Offset)
		// Flux<EventDto> history = reactiveRedisTemplate.opsForStream().read(...)

		// Mode A: 訂閱實時 Pub/Sub 轉發
		return sseEmitterManager.subscribe(sessionId)
				.map(event -> ServerSentEvent.<EventDto>builder()
						.id(event.getInteractionId())
						.event("chat")
						.data(event)
						.build())
				.doOnTerminate(() -> logger.info("[RE-SSE] Stream terminated: sessionId={}", sessionId));
	}

	/**
	 * 發送訊息 — 取代 STOMP @MessageMapping
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
						
						return chatRedisService.enqueueRequest(sessionId, record)
								.map(msgId -> ResponseEntity.ok((Object) Map.of("interactionId", interactionId)));
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
	 * 查詢排隊位置 — 近似值 = stream 長度 - consumer 數量
	 */
	@GetMapping("/chat/{sessionId}/queue-position")
	public Mono<ResponseEntity<Object>> queuePosition(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return Mono.just(ResponseEntity.badRequest().body((Object) "invalid sessionId"));
		}

		// 使用 XINFO GROUPS 獲取 lag
		return reactiveRedisTemplate.execute(conn -> conn.streamCommands()
						.xInfoGroups(ByteBuffer.wrap(idv.hzm.app.bot.config.RedisStreamConfig.BOT_INCOMING_STREAM.getBytes())))
				.filter(group -> "bot-consumer-group".equals(group.groupName()))
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
