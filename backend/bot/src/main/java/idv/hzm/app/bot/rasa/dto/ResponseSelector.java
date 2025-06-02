package idv.hzm.app.bot.rasa.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResponseSelector {
    @JsonProperty("all_retrieval_intents")
    private List<String> allRetrievalIntents;

    private Map<String, SelectorDetail> defaultSelector;

    @JsonProperty("default")
    public void setDefaultSelector(SelectorDetail detail) {
        this.defaultSelector = Map.of("default", detail);
    }

    @JsonProperty("default")
    public SelectorDetail getDefaultSelector() {
        return this.defaultSelector.get("default");
    }

	public List<String> getAllRetrievalIntents() {
		return allRetrievalIntents;
	}

	public void setAllRetrievalIntents(List<String> allRetrievalIntents) {
		this.allRetrievalIntents = allRetrievalIntents;
	}

	public void setDefaultSelector(Map<String, SelectorDetail> defaultSelector) {
		this.defaultSelector = defaultSelector;
	}

	@Override
	public String toString() {
		return "ResponseSelector [allRetrievalIntents=" + allRetrievalIntents + ", defaultSelector=" + defaultSelector
				+ "]";
	}

    // Optional: override getter/setter for other keys if needed
    
}

