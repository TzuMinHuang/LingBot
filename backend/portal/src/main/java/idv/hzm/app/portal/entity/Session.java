package idv.hzm.app.portal.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class Session {

	@Id
	@Column(name = "id", nullable = false, updatable = false)
	private String id;
	
	@Column(name = "user_id", nullable = false)
	private String userId; // 參與者列表（客戶與客服）
	
	@Column(name = "agent_id")
	private String agentId; // 參與者列表（客戶與客服）
	
	@Column(name = "started_at")
	private Instant startTime; // 會話開始時間
	
	@Column(name = "ended_at")
	private Instant endTime; // 會話結束時間
	
	@Column(name = "status", nullable = false)
	private String status;// 會話狀態（例如：active, closed, escalated

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

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public void setEndTime(Instant endTime) {
		this.endTime = endTime;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Session [id=" + id + ", userId=" + userId + ", agentId=" + agentId + ", startTime=" + startTime
				+ ", endTime=" + endTime + ", status=" + status + "]";
	}
}
