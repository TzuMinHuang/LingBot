package idv.hzm.app.bot.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_message", indexes = {
	@Index(name = "idx_cm_session_created", columnList = "session_id, created_at"),
	@Index(name = "idx_cm_interaction", columnList = "interaction_id")
})
public class ChatMessage {

	@Id
	@Column(name = "message_id", nullable = false, updatable = false, length = 36)
	private String messageId;

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "interaction_id", length = 36)
	private String interactionId;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private MessageRole role;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", length = 20)
	private MessageStatus status;

	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	@Column(name = "token_count")
	private Integer tokenCount;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "metadata", columnDefinition = "jsonb")
	private String metadata;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public ChatMessage() {
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getInteractionId() {
		return interactionId;
	}

	public void setInteractionId(String interactionId) {
		this.interactionId = interactionId;
	}

	public MessageRole getRole() {
		return role;
	}

	public void setRole(MessageRole role) {
		this.role = role;
	}

	public MessageStatus getStatus() {
		return status;
	}

	public void setStatus(MessageStatus status) {
		this.status = status;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getTokenCount() {
		return tokenCount;
	}

	public void setTokenCount(Integer tokenCount) {
		this.tokenCount = tokenCount;
	}

	public String getMetadata() {
		return metadata;
	}

	public void setMetadata(String metadata) {
		this.metadata = metadata;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
