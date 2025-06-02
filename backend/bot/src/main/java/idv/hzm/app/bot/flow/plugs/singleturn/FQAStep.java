package idv.hzm.app.bot.flow.plugs.singleturn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.StepStrategy;
import idv.hzm.app.bot.service.RespondUserService;

@Service
public class FQAStep implements StepStrategy {

	@Autowired
	private RespondUserService replyUserService;

	@Override
	public boolean handle(ProcessContext ctx) {
		this.replyUserService.respondToUserByIntent(ctx.getSessionId(), ctx.getIntent());
		return true;
	}

}
