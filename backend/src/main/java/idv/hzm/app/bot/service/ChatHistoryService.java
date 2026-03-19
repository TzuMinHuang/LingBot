package idv.hzm.app.bot.service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.entity.ChatMessage;
import idv.hzm.app.bot.entity.MessageRole;
import idv.hzm.app.bot.entity.MessageStatus;
import idv.hzm.app.bot.entity.Session;
import idv.hzm.app.bot.repo.ChatMessageRepository;
import idv.hzm.app.bot.repo.SessionsRepository;

@Service
public class ChatHistoryService {

	private static final Logger logger = LoggerFactory.getLogger(ChatHistoryService.class);

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private SessionsRepository sessionsRepository;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * Process incoming user message: generate interactionId and persist asynchronously.
	 *
	 * @return interactionId for pairing with assistant response
	 */
	public String processIncomingMessage(String sessionId, String content) {
		String interactionId = UUID.randomUUID().toString();

		ChatMessage msg = new ChatMessage();
		msg.setMessageId(UUID.randomUUID().toString());
		msg.setSessionId(sessionId);
		msg.setInteractionId(interactionId);
		msg.setRole(MessageRole.USER);
		msg.setContent(content);
		msg.setCreatedAt(Instant.now());

		saveMessageAsync(msg);
		return interactionId;
	}

	/**
	 * Save the assistant response with a pre-generated messageId and status.
	 */
	public void saveAssistantMessage(String sessionId, String interactionId, String messageId,
			String fullContent, List<Map<String, Object>> sources, MessageStatus status) {

		ChatMessage msg = new ChatMessage();
		msg.setMessageId(messageId != null ? messageId : UUID.randomUUID().toString());
		msg.setSessionId(sessionId);
		msg.setInteractionId(interactionId);
		msg.setRole(MessageRole.ASSISTANT);
		msg.setContent(fullContent);
		msg.setStatus(status);
		msg.setCreatedAt(Instant.now());

		if (sources != null && !sources.isEmpty()) {
			try {
				msg.setMetadata(objectMapper.writeValueAsString(Map.of("sources", sources)));
			} catch (JsonProcessingException e) {
				logger.warn("Failed to serialize sources metadata: {}", e.getMessage());
			}
		}

		saveMessageAsync(msg);
	}

	/**
	 * Get session history formatted per API spec.
	 */
	public Map<String, Object> getSessionHistory(String sessionId) {
		Session session = sessionsRepository.findById(sessionId).orElse(null);
		List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

		List<Map<String, Object>> history = messages.stream().map(m -> {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("messageId", m.getMessageId());
			item.put("interactionId", m.getInteractionId());
			item.put("role", m.getRole());
			item.put("content", m.getContent());
			item.put("createdAt", m.getCreatedAt().toString());
			return item;
		}).toList();

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("sessionId", sessionId);
		result.put("userId", session != null ? session.getUserId() : null);
		result.put("history", history);
		return result;
	}

	@Async
	public void saveMessageAsync(ChatMessage message) {
		try {
			chatMessageRepository.save(message);
			logger.debug("Saved {} message: sessionId={}, interactionId={}",
					message.getRole(), message.getSessionId(), message.getInteractionId());
		} catch (Exception e) {
			logger.error("Failed to persist chat message: {}", e.getMessage());
		}
	}
}
