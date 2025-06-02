package idv.hzm.app.bot.flow.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

//=== 8. 註冊中心 ===
@Component
public class ProcessRegistry {

	private final Map<String, ProcessTemplate> templates = new HashMap<>();

	public ProcessRegistry(List<ProcessTemplate> templateBeans) {
		for (ProcessTemplate template : templateBeans) {
			String key = template.getClass().getAnnotation(Component.class).value();
			this.templates.put(key, template);
		}
	}

	public ProcessTemplate get(String key) {
		return this.templates.get(key);
	}

}
