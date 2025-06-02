package idv.hzm.app.portal.service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.common.dto.UserDto;
import idv.hzm.app.portal.entity.Session;

@Service
public class ChatRedisService {

	@Autowired
	private StringRedisTemplate redisTemplate;
	@Autowired
	RedisTemplate<String, Object> redisTemplateForObject;

	@Autowired
	private ObjectMapper objectMapper; // Spring Boot 預設有載入 Jackson

	private static final Duration TTL_5_MIN = Duration.ofMinutes(5);

	private static final String SESSION_PREFIX = "chat:session:";// 每個聊天的 Session
	private static final String USER_PREFIX = "user:";// 儲存使用者資訊
	private static final String AGENT_PREFIX = "agent:";// 儲存客服狀態（可擴充）
	private static final String ROOM_PREFIX = "chat:room:";// 每個聊天室的歷史訊息
	private static final String ROOM_BIND = "room:bind:";// 紀錄 room ↔ user/agent
	private static final String QUEUE_WAITING = "queue:waiting";// ZSet 儲存等待排序
	private static final String ONLINE_AGENTS = "online:agents";// Set 儲存在線客服 ID
	private static final String BIND_PREFIX = "bind:user:";// 綁定該使用者給哪位客服

	// ----- 每個聊天的 Session -----

	public String saveChatSession(Session session) {
		String sessionId = session.getId();
		String key = SESSION_PREFIX + sessionId;
		Map<String, Object> chatSessionJson = this.objectMapper.convertValue(session, Map.class);
		this.redisTemplateForObject.opsForHash().putAll(key, chatSessionJson);
		this.redisTemplateForObject.expire(key, TTL_5_MIN);
		return sessionId;
	}

	public Session getChatSession(String sessionId) {
		Map<Object, Object> chatSessionMap = this.redisTemplateForObject.opsForHash().entries(SESSION_PREFIX + sessionId);
		return this.objectMapper.convertValue(chatSessionMap, Session.class);
	}

	// ----- 使用者資料 -----

	public String saveUserInfo(String userId, UserDto user) {
		String key = USER_PREFIX + userId;
		Map<String, String> userInfo = this.objectMapper.convertValue(user, Map.class);
		redisTemplate.opsForHash().putAll(key, userInfo);
		redisTemplate.expire(key, TTL_5_MIN);
		return userId;
	}

	// ----- 聊天訊息 -----

	public void appendChatMessage(String roomId, Object messageObj) {
		try {
			String msg = objectMapper.writeValueAsString(messageObj);
			redisTemplate.opsForList().rightPush(ROOM_PREFIX + roomId + ":msgs", msg);
			redisTemplate.expire(ROOM_PREFIX + roomId + ":msgs", TTL_5_MIN);
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
		redisTemplate.expire(ROOM_BIND + roomId, TTL_5_MIN);
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
		redisTemplate.opsForValue().set(BIND_PREFIX + userId, agentId, TTL_5_MIN);
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
}
