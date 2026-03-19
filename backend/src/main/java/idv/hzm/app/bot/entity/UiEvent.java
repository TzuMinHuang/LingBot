package idv.hzm.app.bot.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ui_event", indexes = {
	@Index(name = "idx_ue_session_seq", columnList = "session_id, seq_index")
}, uniqueConstraints = {
	@UniqueConstraint(name = "uk_ue_session_seq", columnNames = {"session_id", "seq_index"})
})
public class UiEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "session_id", nullable = false, length = 36)
	private String sessionId;

	@Column(name = "seq_index", nullable = false)
	private int seqIndex;

	@Column(name = "event_type", nullable = false, length = 50)
	private String eventType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "payload", columnDefinition = "jsonb")
	private String payload;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	public UiEvent() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public int getSeqIndex() {
		return seqIndex;
	}

	public void setSeqIndex(int seqIndex) {
		this.seqIndex = seqIndex;
	}

	public String getEventType() {
		return eventType;
	}

	public void setEventType(String eventType) {
		this.eventType = eventType;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
