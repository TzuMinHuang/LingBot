package idv.hzm.app.bot.entity;

import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "topic")
public class Topic {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "name", nullable = false, length = 255)
	private String name;

	@Column(name = "description", length = 255)
	private String description;

	@Column(name = "status", length = 20)
	private String status = "active";

	@Column(name = "created_at")
	private ZonedDateTime createdAt = ZonedDateTime.now();

	@Column(name = "updated_at")
	private ZonedDateTime updatedAt = ZonedDateTime.now();

	@OneToMany(mappedBy = "topic")
	private List<Subtopic> subtopics;

	// Constructors
	public Topic() {
	}

	public Topic(String name, String description) {
		this.name = name;
		this.description = description;
		this.status = "active";
		this.createdAt = ZonedDateTime.now();
		this.updatedAt = ZonedDateTime.now();
	}

	// Getters and setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public ZonedDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(ZonedDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public List<Subtopic> getTopicIntents() {
		return subtopics;
	}

	public void setTopicIntents(List<Subtopic> subtopics) {
		this.subtopics = subtopics;
	}

}
