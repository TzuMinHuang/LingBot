package idv.hzm.app.bot.action;

import org.springframework.stereotype.Component;

import idv.hzm.app.bot.dto.FqaDto;

@Component
public class FqaAction extends Action<FqaDto> {

	@Override
	public String getType() {
		return "FQA";
	}

	@Override
	protected FqaDto handle(FqaDto data) {
		return data;
	}

}
