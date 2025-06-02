package idv.hzm.app.bot.flow.core;

import java.util.List;

public abstract class BaseProcess extends ProcessTemplate {
	// 一次說完 ㄧ次跑完所有流程
	// 說一部分 有順序,無順序

	public void run(ProcessContext processContext) {
		List<StepCommand> stepCommandList = defineSteps();
		boolean[] stepStatus = new boolean[stepCommandList.size()];
		for (int i = 0; i < stepCommandList.size(); i++) {
			StepCommand stepCommand = stepCommandList.get(i);
			if (stepCommand.execute(processContext)) {
				//stepStatus
				if (i + 1 < stepCommandList.size()) {
					processContext.setStatus("RUN");
				} else {
					processContext.setStatus("DOWN");
				}
			}

		}
		if ("DOWN".equals(processContext.getStatus())) {
			onComplete(processContext);
		}
	}
}
