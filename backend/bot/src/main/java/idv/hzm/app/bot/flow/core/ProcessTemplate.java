package idv.hzm.app.bot.flow.core;

import java.util.List;

public abstract class ProcessTemplate {
	protected abstract List<StepCommand> defineSteps();

	public abstract void run(ProcessContext processContext);

	protected abstract void onComplete(ProcessContext processContext);

	protected abstract void onFailure(ProcessContext processContext);

}
