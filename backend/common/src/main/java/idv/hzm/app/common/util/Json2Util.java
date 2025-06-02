package idv.hzm.app.common.util;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;


public class Json2Util {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	// Object → JSON 字串
	public static String toJson(Object obj) {
		try {
			return OBJECT_MAPPER.writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException("轉換為 JSON 失敗", e);
		}
	}

	// JSON 字串 → Object（指定類型）
	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			return OBJECT_MAPPER.readValue(json, clazz);
		} catch (Exception e) {
			throw new RuntimeException("JSON 轉換為物件失敗", e);
		}
	}
	
	public static <T> T extractData(Object rawData, Class<T> clazz) {

	    return OBJECT_MAPPER.convertValue(rawData, clazz);
	}
	
	public static <T> List<T> extractList(Object rawData, Class<T> clazz) {

	    CollectionType listType = OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz);
	    return OBJECT_MAPPER.convertValue(rawData, listType);
	}
}
