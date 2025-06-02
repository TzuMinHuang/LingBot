package idv.hzm.app.bot.flow.core;

import java.util.HashMap;
import java.util.Map;

public class ProcessContext {
	private String sessionId;
	private String content;
	private String intent;
	private String subtopic;
	private String topicName;
	private Map<String, Object> data = new HashMap<>();
	private String status;
	private String responseText;

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getIntent() {
		return intent;
	}

	public void setIntent(String intent) {
		this.intent = intent;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getResponseText() {
		return responseText;
	}

	public void setResponseText(String responseText) {
		this.responseText = responseText;
	}

	public void set(String key, Object value) {
		data.put(key, value);
	}

	public Object get(String key) {
		return data.get(key);
	}

	public Map<String, Object> getData() {
		return data;
	}

	public void setData(Map<String, Object> data) {
		this.data = data;
	}

	public String getSubtopic() {
		return subtopic;
	}

	public void setSubtopic(String subtopic) {
		this.subtopic = subtopic;
	}

	@Override
	public String toString() {
		return "ProcessContext [sessionId=" + sessionId + ", content=" + content + ", intent=" + intent + ", subtopic="
				+ subtopic + ", topicName=" + topicName + ", data=" + data + ", status=" + status + ", responseText="
				+ responseText + "]";
	}

}
