package idv.hzm.app.bot.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {

	public static final String TYPE_MESSAGE = "MESSAGE";
	public static final String TYPE_STREAM_CHUNK = "STREAM_CHUNK";
	public static final String TYPE_STREAM_END = "STREAM_END";
	public static final String TYPE_PROCESSING_START = "PROCESSING_START";
	public static final String TYPE_QUEUE_UPDATE = "QUEUE_UPDATE";

	private String sessionId;
	private String interactionId;
	private String type = TYPE_MESSAGE;
	private BasePayload payload; // 使用 Jackson 的 JsonNode 來對應 JSONB 欄位

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getInteractionId() {
		return interactionId;
	}

	public void setInteractionId(String interactionId) {
		this.interactionId = interactionId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public BasePayload getPayload() {
		return payload;
	}

	public void setPayload(BasePayload payload) {
		this.payload = payload;
	}

	@Override
	public int hashCode() {
		return Objects.hash(payload, sessionId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EventDto other = (EventDto) obj;
		return Objects.equals(payload, other.payload) && Objects.equals(sessionId, other.sessionId);
	}

	@Override
	public String toString() {
		return "EventDto [sessionId=" + sessionId + ", payload=" + payload + "]";
	}

}
