package idv.hzm.app.bot.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "chat_session", indexes = {
	@Index(name = "idx_cs_user_id", columnList = "user_id"),
	@Index(name = "idx_cs_created_at", columnList = "created_at")
})
public class Session {

	@Id
	@Column(name = "id", nullable = false, updatable = false, length = 36)
	private String id;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@Column(name = "agent_id")
	private String agentId;

	@Column(name = "title")
	private String title;

	@Column(name = "created_at")
	private Instant startTime;

	@Column(name = "expires_at")
	private Instant expiresAt;

	@Column(name = "ended_at")
	private Instant endTime;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private SessionStatus status;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public SessionStatus getStatus() {
		return status;
	}

	public void setStatus(SessionStatus status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Session [id=" + id + ", userId=" + userId + ", title=" + title + ", status=" + status
				+ ", startTime=" + startTime + ", expiresAt=" + expiresAt + "]";
	}
}
