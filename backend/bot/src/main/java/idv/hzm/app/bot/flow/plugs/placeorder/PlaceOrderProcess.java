package idv.hzm.app.bot.flow.plugs.placeorder;

import java.util.List;

import org.springframework.stereotype.Component;

import idv.hzm.app.bot.flow.core.ProcessBuilder;
import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.SpecifyProcess;
import idv.hzm.app.bot.flow.core.StepCommand;

//=== 6. 流程樣板實作 ===
@Component("PLACE_ORDER")
public class PlaceOrderProcess extends SpecifyProcess {
	@Override
	protected List<StepCommand> defineSteps() {
		return new ProcessBuilder().add("fill_address", new FillAddressStepStrategy())
				.add("input_name", new InputNameStepStrategy()).build();
	}

	@Override
	protected void onComplete(ProcessContext processContext) {

	}

	@Override
	protected void onFailure(ProcessContext processContext) {
		// TODO Auto-generated method stub
		
	}

}
