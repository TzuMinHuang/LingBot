package idv.hzm.app.bot.flow.core;

public class StepCommandSimple implements StepCommand {
	private final StepStrategy strategy;
	private final String stepName;

	public StepCommandSimple(String stepName, StepStrategy strategy) {
		this.stepName = stepName;
		this.strategy = strategy;
	}

	@Override
	public boolean execute(ProcessContext context) {
		return this.strategy.handle(context); // 呼叫實際邏輯
	}

	@Override
	public String getName() {
		return this.stepName;
	}
}
