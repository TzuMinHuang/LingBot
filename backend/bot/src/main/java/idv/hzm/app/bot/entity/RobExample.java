package idv.hzm.app.bot.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rob_examples")
public class RobExample {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "example_text", nullable = false)
	private String exampleText;

	@ManyToOne
	@JoinColumn(name = "intent_name", referencedColumnName = "name", nullable = false)
	private RobIntent intent;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getExampleText() {
		return exampleText;
	}

	public void setExampleText(String exampleText) {
		this.exampleText = exampleText;
	}

	public RobIntent getIntent() {
		return intent;
	}

	public void setIntent(RobIntent intent) {
		this.intent = intent;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
