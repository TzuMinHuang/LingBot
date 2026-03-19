package idv.hzm.app.bot.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "message_chunk", indexes = {
	@Index(name = "idx_mc_message_seq", columnList = "message_id, seq_index")
})
public class MessageChunk {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", nullable = false, length = 36)
	private String messageId;

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "interaction_id", length = 36)
	private String interactionId;

	@Column(name = "seq_index", nullable = false)
	private int seqIndex;

	@Column(name = "content", columnDefinition = "TEXT")
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public MessageChunk() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public int getSeqIndex() {
		return seqIndex;
	}

	public void setSeqIndex(int seqIndex) {
		this.seqIndex = seqIndex;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
