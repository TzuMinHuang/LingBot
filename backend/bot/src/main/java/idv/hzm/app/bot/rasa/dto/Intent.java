package idv.hzm.app.bot.rasa.dto;

public class Intent {
	private String name;
	private double confidence;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	@Override
	public String toString() {
		return "Intent [name=" + name + ", confidence=" + confidence + "]";
	}

	// getters and setters
}
