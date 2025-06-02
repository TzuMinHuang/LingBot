package idv.hzm.app.bot.flow.plugs.queryorder;

import org.springframework.stereotype.Service;

import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.StepStrategy;

@Service
public class ProvideOrderIdStep implements StepStrategy {

	@Override
	public boolean handle(ProcessContext ctx) {
		System.out.println("執行 GetOrderIdStepStrategy");
		ctx.set("response_text", "請提供訂單ID");;
		return false;
	}

}
