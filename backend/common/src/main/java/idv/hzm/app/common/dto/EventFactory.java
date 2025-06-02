package idv.hzm.app.common.dto;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventFactory {

	public static EventDto createEvent(String json) {
		try {
			System.out.println(json);
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json, EventDto.class); // 自動多型解析
		} catch (Exception e) {
			throw new RuntimeException("Invalid payload JSON", e);
		}
	}

	public static EventDto createEvent(String sessionId, BasePayload payload) {
		try {
			EventDto newEventDto = new EventDto();
			if (payload instanceof MessagePayload) {
				newEventDto.setSessionId(sessionId);
				newEventDto.setPayload((MessagePayload) payload);
			}
			return newEventDto;
		} catch (Exception e) {
			throw new RuntimeException("Invalid payload JSON", e);
		}
	}

	public static EventDto createEvent(String sessionId, Map<String, Object> basePayload) {
		try {
			EventDto newEventDto = new EventDto();
			newEventDto.setSessionId(sessionId);
			if ("message".equals(basePayload.get("type"))) {
				MessagePayload messagePayload = new MessagePayload();
				messagePayload.setContent((String) basePayload.get("content"));
				newEventDto.setPayload(messagePayload);
			}
			return newEventDto;
		} catch (Exception e) {
			throw new RuntimeException("Invalid payload JSON", e);
		}
	}
}
