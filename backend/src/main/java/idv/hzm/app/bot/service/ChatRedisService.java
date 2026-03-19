package idv.hzm.app.bot.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.dto.UserDto;
import idv.hzm.app.bot.entity.Session;

@Service
public class ChatRedisService {

	@Autowired
	private StringRedisTemplate redisTemplate;
	@Autowired
	RedisTemplate<String, Object> redisTemplateForObject;
	@Autowired
	private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

	@Autowired
	private ObjectMapper objectMapper; // Spring Boot 預設有載入 Jackson

	private static final Duration SESSION_TTL = Duration.ofHours(24);
	private static final Duration CONTEXT_TTL = Duration.ofHours(1);

	private static final String SESSION_PREFIX = "chat:session:";// 每個聊天的 Session
	private static final String USER_PREFIX = "user:";// 儲存使用者資訊
	private static final String AGENT_PREFIX = "agent:";// 儲存客服狀態（可擴充）
	private static final String ROOM_PREFIX = "chat:room:";// 每個聊天室的歷史訊息
	private static final String ROOM_BIND = "room:bind:";// 紀錄 room ↔ user/agent
	private static final String QUEUE_WAITING = "queue:waiting";// ZSet 儲存等待排序
	private static final String ONLINE_AGENTS = "online:agents";// Set 儲存在線客服 ID
	private static final String BIND_PREFIX = "bind:user:";// 綁定該使用者給哪位客服
	private static final String ACTIVE_REQUEST_PREFIX = "chat:active:"; // 儲存活躍請求的 key
	private static final String CONTROL_SIGNAL_PREFIX = "chat:control:"; // PAUSE/CANCEL 控制信號

	// ----- 每個聊天的 Session -----

	public String saveChatSession(Session session) {
		String sessionId = session.getId();
		String key = SESSION_PREFIX + sessionId;
		Map<String, Object> chatSessionJson = this.objectMapper.convertValue(session, Map.class);
		this.redisTemplateForObject.opsForHash().putAll(key, chatSessionJson);
		this.redisTemplateForObject.expire(key, SESSION_TTL);
		return sessionId;
	}

	public boolean isSessionActive(String sessionId) {
		return Boolean.TRUE.equals(this.redisTemplateForObject.hasKey(SESSION_PREFIX + sessionId));
	}

	public Session getChatSession(String sessionId) {
		Map<Object, Object> chatSessionMap = this.redisTemplateForObject.opsForHash()
				.entries(SESSION_PREFIX + sessionId);
		return this.objectMapper.convertValue(chatSessionMap, Session.class);
	}

	// ----- 使用者資料 -----

	public String saveUserInfo(String userId, UserDto user) {
		String key = USER_PREFIX + userId;
		Map<String, String> userInfo = this.objectMapper.convertValue(user, Map.class);
		redisTemplate.opsForHash().putAll(key, userInfo);
		redisTemplate.expire(key, CONTEXT_TTL);
		return userId;
	}

	// ----- 聊天訊息 -----

	public void appendChatMessage(String roomId, Object messageObj) {
		try {
			String msg = objectMapper.writeValueAsString(messageObj);
			redisTemplate.opsForList().rightPush(ROOM_PREFIX + roomId + ":msgs", msg);
			redisTemplate.expire(ROOM_PREFIX + roomId + ":msgs", CONTEXT_TTL);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("訊息序列化失敗", e);
		}
	}

	public <T> List<T> getChatMessages(String roomId, int limit, Class<T> clazz) {
		List<String> raw = redisTemplate.opsForList().range(ROOM_PREFIX + roomId + ":msgs", 0, limit - 1);
		if (raw == null)
			return Collections.emptyList();
		return raw.stream().map(msg -> {
			try {
				return objectMapper.readValue(msg, clazz);
			} catch (JsonProcessingException e) {
				return null;
			}
		}).filter(Objects::nonNull).toList();
	}

	// ----- Room 綁定 -----

	public void bindRoom(String roomId, String userId, String agentId) {
		redisTemplate.opsForHash().put(ROOM_BIND + roomId, "userId", userId);
		redisTemplate.opsForHash().put(ROOM_BIND + roomId, "agentId", agentId);
		redisTemplate.expire(ROOM_BIND + roomId, CONTEXT_TTL);
	}

	public Map<Object, Object> getRoomBinding(String roomId) {
		return redisTemplate.opsForHash().entries(ROOM_BIND + roomId);
	}

	public void destroyRoom(String roomId) {
		redisTemplate.delete(ROOM_PREFIX + roomId + ":msgs");
		redisTemplate.delete(ROOM_BIND + roomId);
	}

	// ----- 排隊與綁定 -----

	public void addToWaitingQueue(String userId, long timestamp) {
		redisTemplate.opsForZSet().add(QUEUE_WAITING, userId, timestamp);
	}

	public String getFirstWaitingUser() {
		Set<String> users = redisTemplate.opsForZSet().range(QUEUE_WAITING, 0, 0);
		return users != null && !users.isEmpty() ? users.iterator().next() : null;
	}

	public void removeFromQueue(String userId) {
		redisTemplate.opsForZSet().remove(QUEUE_WAITING, userId);
	}

	public void bindUserToAgent(String userId, String agentId) {
		redisTemplate.opsForValue().set(BIND_PREFIX + userId, agentId, CONTEXT_TTL);
	}

	public String getAgentForUser(String userId) {
		return redisTemplate.opsForValue().get(BIND_PREFIX + userId);
	}

	public void unbindUser(String userId) {
		redisTemplate.delete(BIND_PREFIX + userId);
	}

	public void setAgentOnline(String agentId) {
		redisTemplate.opsForSet().add(ONLINE_AGENTS, agentId);
	}

	public void setAgentOffline(String agentId) {
		redisTemplate.opsForSet().remove(ONLINE_AGENTS, agentId);
	}

	public Set<String> getOnlineAgents() {
		return redisTemplate.opsForSet().members(ONLINE_AGENTS);
	}

	// ----- 冪等性與鎖 -----

	/**
	 * 嘗試獲取請求鎖，成功則回傳 true，代表可以進行提問
	 */
	public boolean acquireRequestLock(String sessionId) {
		String key = ACTIVE_REQUEST_PREFIX + sessionId;
		// 設置 10 分鐘過期，防止意外死鎖
		return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, "processing", Duration.ofMinutes(10)));
	}

	/**
	 * 釋放請求鎖
	 */
	public void releaseRequestLock(String sessionId) {
		redisTemplate.delete(ACTIVE_REQUEST_PREFIX + sessionId);
	}

	/**
	 * 檢查是否正在處理中 (Reactive)
	 */
	public Mono<Boolean> checkIdempotency(String interactionId) {
		String key = "chat:interaction:" + interactionId;
		return reactiveRedisTemplate.hasKey(key);
	}

	// ----- 控制信號（PAUSE/CANCEL）-----

	/**
	 * 設定控制信號，供 RequestConsumer 輪詢檢查。60 秒後自動過期。
	 */
	public void setControlSignal(String sessionId, String signal) {
		redisTemplate.opsForValue().set(CONTROL_SIGNAL_PREFIX + sessionId, signal, Duration.ofSeconds(60));
	}

	/**
	 * 讀取並刪除控制信號（原子操作，使用 GETDEL 避免 race condition）。
	 */
	public String consumeControlSignal(String sessionId) {
		String key = CONTROL_SIGNAL_PREFIX + sessionId;
		return redisTemplate.opsForValue().getAndDelete(key);
	}

	/**
	 * 將請求加入串流 (Reactive)
	 */
	public Mono<String> enqueueRequest(String sessionId, Map<String, String> record) {
		return reactiveRedisTemplate.opsForStream()
				.add(idv.hzm.app.bot.config.RedisStreamConfig.REQUEST_STREAM, record)
				.map(id -> id.getValue());
	}
}
