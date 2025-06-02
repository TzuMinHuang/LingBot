package idv.hzm.app.bot.flow.core;

public interface StepCommand {
	String getName();
	boolean execute(ProcessContext context);
}
