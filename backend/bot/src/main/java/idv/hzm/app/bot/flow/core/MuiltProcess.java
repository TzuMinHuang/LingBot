package idv.hzm.app.bot.flow.core;

import java.util.List;

public abstract class MuiltProcess extends ProcessTemplate {

	public void run(ProcessContext processContext) {
		List<StepCommand> stepCommandList = defineSteps();
		boolean isRun = false;
		for (int i = 0; i < stepCommandList.size(); i++) {
			StepCommand stepCommand = stepCommandList.get(i);
			if (stepCommand.getName().equals(processContext.getIntent())) {
				isRun = true;
			}
			if (isRun) {
				if (stepCommand.execute(processContext)) {
					if (i + 1 < stepCommandList.size()) {
						processContext.setIntent(stepCommandList.get(i + 1).getName());
						processContext.setStatus("RUN");
					} else {
						processContext.setStatus("DOWN");
					}
				}
			}

		}
		if ("DOWN".equals(processContext.getStatus())) {
			onComplete(processContext);
		}
	}

}
