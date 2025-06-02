package idv.hzm.app.bot.rasa.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RasaNluResponse {

    private String text;
    private Intent intent;
    
    @JsonProperty("entities")
    private List<Entity> entities;

    @JsonProperty("text_tokens")
    private List<List<Integer>> textTokens;

    @JsonProperty("intent_ranking")
    private List<Intent> intentRanking;

    @JsonProperty("response_selector")
    private ResponseSelector responseSelector;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Intent getIntent() {
		return intent;
	}

	public void setIntent(Intent intent) {
		this.intent = intent;
	}

	public List<Entity> getEntities() {
		return entities;
	}

	public void setEntities(List<Entity> entities) {
		this.entities = entities;
	}

	public List<List<Integer>> getTextTokens() {
		return textTokens;
	}

	public void setTextTokens(List<List<Integer>> textTokens) {
		this.textTokens = textTokens;
	}

	public List<Intent> getIntentRanking() {
		return intentRanking;
	}

	public void setIntentRanking(List<Intent> intentRanking) {
		this.intentRanking = intentRanking;
	}

	public ResponseSelector getResponseSelector() {
		return responseSelector;
	}

	public void setResponseSelector(ResponseSelector responseSelector) {
		this.responseSelector = responseSelector;
	}

	@Override
	public String toString() {
		return "RasaNluResponse [text=" + text + ", intent=" + intent + ", entities=" + entities + ", textTokens="
				+ textTokens + ", intentRanking=" + intentRanking + ", responseSelector=" + responseSelector + "]";
	}
    
}

