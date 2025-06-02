package idv.hzm.app.bot.flow.plugs.queryorder;

import org.springframework.stereotype.Service;

import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.StepStrategy;


@Service
public class QueryOrderStep implements StepStrategy {

	@Override
	public boolean handle(ProcessContext ctx) {
		ctx.set("response_text", "請提供訂單ID");;
		return true;
	}

}
