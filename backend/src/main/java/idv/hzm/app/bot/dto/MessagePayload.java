package idv.hzm.app.bot.dto;

import java.util.List;
import java.util.Map;

public class MessagePayload extends BasePayload {
	private String content;
	private List<Map<String, Object>> sources;

	public MessagePayload() {
		this("");
	}

	public MessagePayload(String content) {
		this.content = content;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<Map<String, Object>> getSources() {
		return sources;
	}

	public void setSources(List<Map<String, Object>> sources) {
		this.sources = sources;
	}
}
