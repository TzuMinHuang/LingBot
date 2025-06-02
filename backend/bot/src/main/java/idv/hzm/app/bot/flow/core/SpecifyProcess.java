package idv.hzm.app.bot.flow.core;

import java.util.List;

public abstract class SpecifyProcess extends ProcessTemplate {

	public void run(ProcessContext processContext) {
		List<StepCommand> stepCommandList = defineSteps();
		for (int i = 0; i < stepCommandList.size(); i++) {
			StepCommand stepCommand = stepCommandList.get(i);
			if (stepCommand.getName().equals(processContext.getSubtopic())) {
				if (stepCommand.execute(processContext)) {
					onComplete(processContext);
				} else {
					onFailure(processContext);
				}
				processContext.setStatus("DOWN");
				break;
			}
		}
	}

}
