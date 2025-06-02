package idv.hzm.app.bot.flow.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class ProcessBuilder {

	private List<StepCommand> steps = new ArrayList<>();

	private ProcessBuilder add(StepCommand stepCommand) {
		this.steps.add(stepCommand);
		return this;
	}

	public List<StepCommand> build() {
		return this.steps;
	}

	public ProcessBuilder add(String commandName, StepStrategy stepStrategy) {

		return this.add(new StepCommandSimple(commandName, stepStrategy));
	}

	public ProcessBuilder addIf(String commandName, Predicate<ProcessContext> condition,
			StepStrategy trueStepStrategy) {
		return this.addIfElse(commandName, condition, trueStepStrategy, ctx -> {
			return false;
		});
	}

	public ProcessBuilder addIfElse(String commandName, Predicate<ProcessContext> condition,
			StepStrategy trueStepStrategy, StepStrategy falseStepStrategy) {
		return this.add(new StepCommandConditional(commandName, condition, trueStepStrategy, falseStepStrategy));
	}

}
