package idv.hzm.app.bot.flow.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


//=== 7. ProcessEngine (State Pattern) ===
@Service
public class ProcessEngine {

	@Autowired
	private ProcessRegistry processRegistry;

	public void execute(ProcessContext context) {

		String processName = context.getTopicName();
		ProcessTemplate processTemplate = this.processRegistry.get(processName);
		if (processTemplate == null) {
			throw new IllegalArgumentException("No process found: " + processName);
		}
		processTemplate.run(context);

	}
}
