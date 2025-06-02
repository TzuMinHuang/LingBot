package idv.hzm.app.bot.flow.plugs.singleturn;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import idv.hzm.app.bot.flow.core.ProcessBuilder;
import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.SpecifyProcess;
import idv.hzm.app.bot.flow.core.StepCommand;
import idv.hzm.app.bot.service.RespondUserService;

//=== 6. 流程樣板實作 ===
@Component("SINGLE_TURN")
public class SingleTurnDialogueProcess extends SpecifyProcess {

	@Autowired
	private FQAStep fQAStep;
	@Autowired
	private TransferToHumanStep transferToHumanStrategy;
	@Autowired
	private CancelTransferStep cancelTransferStrategy;

	@Override
	protected List<StepCommand> defineSteps() {
		return new ProcessBuilder().add("FQA_QUESTION", this.fQAStep)
				.add("TRANSFER_AGENT", this.transferToHumanStrategy)
				.add("CANCEL_TRANSFER_AGENT", this.cancelTransferStrategy).build();
	}

	@Autowired
	private RespondUserService replyUserService;

	@Override
	protected void onComplete(ProcessContext processContext) {
		this.replyUserService.respondToUserByIntent(processContext.getSessionId(), "help_customer");
	}

	@Override
	protected void onFailure(ProcessContext processContext) {
		this.replyUserService.respondToUserByIntent(processContext.getSessionId(), "fallback");
	}

}
