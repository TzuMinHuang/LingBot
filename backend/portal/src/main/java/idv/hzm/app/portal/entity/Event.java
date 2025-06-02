package idv.hzm.app.portal.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "events")
public class Event {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "sessions_id", length = 20)
	private String sessionsId;

	@Column(nullable = false, length = 50)
	private String type; // 'message', 'order_submitted', etc.

	@Column(name = "sender_type", length = 10)
	private String senderType; // 'user', 'agent', 'bot', 'system'

	@Column(name = "sender_id")
	private UUID senderId;

	@Column(columnDefinition = "jsonb", nullable = false)
	private String payload; // 使用 Jackson 的 JsonNode 來對應 JSONB 欄位

	@Column(name = "created_at", columnDefinition = "timestamptz")
	private ZonedDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSessionsId() {
		return sessionsId;
	}

	public void setSessionsId(String sessionsId) {
		this.sessionsId = sessionsId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSenderType() {
		return senderType;
	}

	public void setSenderType(String senderType) {
		this.senderType = senderType;
	}

	public UUID getSenderId() {
		return senderId;
	}

	public void setSenderId(UUID senderId) {
		this.senderId = senderId;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
