package idv.hzm.app.bot.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "intent", uniqueConstraints = { @UniqueConstraint(name = "intent_name_key", columnNames = "name") })
public class Intent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "created_at", columnDefinition = "timestamptz")
	private OffsetDateTime createdAt = OffsetDateTime.now();

	@Column(name = "updated_at", columnDefinition = "timestamptz")
	private OffsetDateTime updatedAt = OffsetDateTime.now();

	@ManyToOne
	@JoinColumn(name = "subtopic_id", nullable = false)
	private Subtopic subtopic;

	// === Getters and Setters ===

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Subtopic getSubtopic() {
		return subtopic;
	}

	public void setSubtopic(Subtopic subtopic) {
		this.subtopic = subtopic;
	}

}
