package idv.hzm.app.bot.flow.core;

import java.util.function.Predicate;

public class StepCommandConditional implements StepCommand {
	private String stepName;
	private Predicate<ProcessContext> condition;
	private StepStrategy trueStep;
	private StepStrategy falseStep;

	public StepCommandConditional(String stepName, Predicate<ProcessContext> condition, StepStrategy trueStep) {
		this.stepName = stepName;
		this.condition = condition;
		this.trueStep = trueStep;
		this.falseStep = ctx -> {
			return false;
		};
	}

	public StepCommandConditional(String stepName, Predicate<ProcessContext> condition, StepStrategy trueStep,
			StepStrategy falseStep) {
		this.stepName = stepName;
		this.condition = condition;
		this.trueStep = trueStep;
		this.falseStep = falseStep;
	}

	@Override
	public boolean execute(ProcessContext context) {
		if (this.condition.test(context)) {
			return this.trueStep.handle(context);
		} else {
			return this.falseStep.handle(context);
		}

	}

	@Override
	public String getName() {
		return this.stepName;
	}
}
