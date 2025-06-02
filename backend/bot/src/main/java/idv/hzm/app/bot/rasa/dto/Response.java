package idv.hzm.app.bot.rasa.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Response {
	private List<Object> responses;
	private double confidence;

	@JsonProperty("intent_response_key")
	private String intentResponseKey;

	@JsonProperty("utter_action")
	private String utterAction;

	public List<Object> getResponses() {
		return responses;
	}

	public void setResponses(List<Object> responses) {
		this.responses = responses;
	}

	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public String getIntentResponseKey() {
		return intentResponseKey;
	}

	public void setIntentResponseKey(String intentResponseKey) {
		this.intentResponseKey = intentResponseKey;
	}

	public String getUtterAction() {
		return utterAction;
	}

	public void setUtterAction(String utterAction) {
		this.utterAction = utterAction;
	}

	@Override
	public String toString() {
		return "Response [responses=" + responses + ", confidence=" + confidence + ", intentResponseKey="
				+ intentResponseKey + ", utterAction=" + utterAction + "]";
	}

	// getters and setters
	
	
}
