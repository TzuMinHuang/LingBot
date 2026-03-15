package idv.hzm.app.bot.controller;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import idv.hzm.app.bot.config.RedisStreamConfig;
import idv.hzm.app.bot.config.SseEmitterManager;
import idv.hzm.app.bot.dto.InitialClientInfo;
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
	private StringRedisTemplate redisTemplate;

	@Autowired
	private SuggestionService suggestionService;

	@Autowired
	private ChatHistoryService chatHistoryService;

	@Autowired
	private ChatRedisService chatRedisService;

	@Autowired
	private RedisStreamConfig redisStreamConfig;

	@PostMapping("/chat/initial")
	public InitialClientInfo initialClientInfo() {
		return this.chatSessionService.initialClientInfo();
	}

	/**
	 * SSE 訂閱 — 前端連線後持續接收事件
	 */
	@GetMapping("/chat/{sessionId}/stream")
	public SseEmitter stream(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "invalid sessionId");
		}
		if (!chatRedisService.isSessionActive(sessionId)) {
			throw new org.springframework.web.server.ResponseStatusException(
					org.springframework.http.HttpStatus.BAD_REQUEST, "session not found or expired");
		}
		logger.info("[SSE] Client connected: sessionId={}", sessionId);
		return sseEmitterManager.create(sessionId);
	}

	/**
	 * 發送訊息 — 取代 STOMP @MessageMapping
	 */
	@PostMapping("/chat/{sessionId}/send")
	public ResponseEntity<?> send(@PathVariable String sessionId, @RequestBody Map<String, String> body) {
		if (!isValidSessionId(sessionId)) {
			return ResponseEntity.badRequest().body("invalid sessionId");
		}
		if (!chatRedisService.isSessionActive(sessionId)) {
			return ResponseEntity.badRequest().body("session not found or expired");
		}

		String content = body.get("content");
		if (content == null || content.isBlank()) {
			return ResponseEntity.badRequest().body("content is required");
		}
		if (content.length() > maxInputLength) {
			logger.warn("Message from session {} exceeds max length ({}), rejected", sessionId, content.length());
			return ResponseEntity.badRequest().body("content exceeds max length");
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
			redisTemplate.opsForStream()
					.add(StreamRecords.mapBacked(record).withStreamKey(RedisStreamConfig.BOT_INCOMING_STREAM));
			return ResponseEntity.ok().build();
		} catch (Exception e) {
			logger.error("Failed to enqueue message to Redis Stream: {}", e.getMessage());
			return ResponseEntity.internalServerError().body("failed to enqueue message");
		}
	}

	/**
	 * 取得對話歷史紀錄
	 */
	@GetMapping("/chat/{sessionId}/history")
	public ResponseEntity<?> history(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return ResponseEntity.badRequest().body("invalid sessionId");
		}
		Map<String, Object> result = chatHistoryService.getSessionHistory(sessionId);
		if (result.get("userId") == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(result);
	}

	/**
	 * 查詢排隊位置 — 近似值 = stream 長度 - consumer 數量
	 */
	@GetMapping("/chat/{sessionId}/queue-position")
	public ResponseEntity<?> queuePosition(@PathVariable String sessionId) {
		if (!isValidSessionId(sessionId)) {
			return ResponseEntity.badRequest().body("invalid sessionId");
		}
		Long len = redisTemplate.opsForStream()
				.size(RedisStreamConfig.BOT_INCOMING_STREAM);
		long streamLen = len != null ? len : 0L;
		int consumers = redisStreamConfig.getConsumerCount();
		int position = Math.max(0, (int) (streamLen - consumers));
		return ResponseEntity.ok(Map.of("position", position));
	}

	private boolean isValidSessionId(String sessionId) {
		return sessionId != null && SESSION_ID_PATTERN.matcher(sessionId).matches();
	}

	/**
	 * 取得常見問題 — 依詢問頻率排序
	 */
	@GetMapping("/chat/suggestions")
	public Map<String, List<String>> suggestions(@RequestParam(defaultValue = "10") int limit) {
		List<String> items = suggestionService.getTopSuggestions(limit);
		return Map.of("suggestions", items);
	}
}
