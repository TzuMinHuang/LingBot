package idv.hzm.app.common.dto;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PayloadFactory {

	public static BasePayload fromJson(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(json, BasePayload.class); // 自動多型解析
		} catch (Exception e) {
			throw new RuntimeException("Invalid payload JSON", e);
		}
	}

	public static String toJson(BasePayload payload) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(payload);
		} catch (Exception e) {
			throw new RuntimeException("Failed to serialize payload", e);
		}
	}
}
