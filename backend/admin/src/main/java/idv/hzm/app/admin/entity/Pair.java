package idv.hzm.app.admin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_user_pair")
public class Pair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "agent_id", nullable = false)
    private String agentId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "paired_at", nullable = false)
    private LocalDateTime pairedAt;

    @Column(name = "status", nullable = false)
    private String status; // e.g. "active", "finished", "failed"

    // Constructors
    public Pair() {}

    public Pair(String agentId, String userId) {
        this.agentId = agentId;
        this.userId = userId;
        this.pairedAt = LocalDateTime.now();
        this.status = "active";
    }

    // Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public LocalDateTime getPairedAt() {
		return pairedAt;
	}

	public void setPairedAt(LocalDateTime pairedAt) {
		this.pairedAt = pairedAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}

