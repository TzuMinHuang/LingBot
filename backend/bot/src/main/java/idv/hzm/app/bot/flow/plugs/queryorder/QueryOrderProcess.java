package idv.hzm.app.bot.flow.plugs.queryorder;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.flow.core.BaseProcess;
import idv.hzm.app.bot.flow.core.ProcessBuilder;
import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.StepCommand;

@Component("QUERY_ORDER")
public class QueryOrderProcess extends BaseProcess {

	@Autowired
	private ProvideOrderIdStep provideOrderIdStep;
	@Autowired
	private QueryOrderStep queryOrderStep;

	@Override
	protected List<StepCommand> defineSteps() {
		return new ProcessBuilder()
				.addIf("QUERY_ORDER", ctx -> isProvideOrderId(ctx), this.provideOrderIdStep)
				.addIf("PROVIDE_ORDER_ID", ctx -> isQueryOrder(ctx), this.queryOrderStep)
				.build();
	}

	private boolean isProvideOrderId(ProcessContext ctx) {
		String orderId = (String) ctx.get("order_id");
		return orderId == null;
	}

	private boolean isQueryOrder(ProcessContext ctx) {
		String orderId = (String) ctx.get("order_id");
		return orderId != null;
	}

	@Override
	protected void onComplete(ProcessContext processContext) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onFailure(ProcessContext processContext) {
		// TODO Auto-generated method stub

	}

}
