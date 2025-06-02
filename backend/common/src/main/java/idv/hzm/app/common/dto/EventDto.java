package idv.hzm.app.common.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EventDto {

	private String sessionId;

	private BasePayload payload; // 使用 Jackson 的 JsonNode 來對應 JSONB 欄位

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
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
