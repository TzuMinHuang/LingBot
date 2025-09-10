package idv.hzm.app.bot.action;

import org.springframework.stereotype.Component;
import idv.hzm.app.bot.dto.LeaveDto;

@Component
public class LeaveAction extends Action<LeaveDto> {

	@Override
	public String getType() {
		return "Leave";
	}

	@Override
	protected LeaveDto handle(LeaveDto data) {

		return data;
	}

}
