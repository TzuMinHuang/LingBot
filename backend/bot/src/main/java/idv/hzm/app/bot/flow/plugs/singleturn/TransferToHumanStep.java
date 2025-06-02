package idv.hzm.app.bot.flow.plugs.singleturn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.client.QueueClient;
import idv.hzm.app.bot.client.dto.QueueInfoDto;
import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.StepStrategy;
import idv.hzm.app.bot.service.RespondUserService;

@Service
public class TransferToHumanStep implements StepStrategy {

	@Autowired
	private QueueClient queueClient;
	@Autowired
	private RespondUserService respondUserService;

	@Override
	public boolean handle(ProcessContext processContext) {
		// 查看客服是否在上班時間，是的話往下執行，不是的話結束處理
		final String sessionId = processContext.getSessionId();
		if (this.isOutsideBusinessHours()) {
			this.respondUserService.respondToUserWithText(sessionId, "目前無此服務，請於上班時間在使用");
			return true;
		}

		if (this.queueClient.getStatus(sessionId)) { // 查看是否已在排隊
			this.respondUserService.respondToUserWithText(sessionId, "請稍候，已轉接給客服，正在排隊中。");
		} else {
			QueueInfoDto queueInfoDto = this.queueClient.enqueue(sessionId); // 將客戶加入連線排隊
			if (queueInfoDto.isAlreadyInQueue()) {
				this.respondUserService.respondToUserWithText(sessionId, "轉接給客服中，正在排隊中，請稍候。");// 回覆顧客已轉接訊息
			}
		}
		return true;
	}

	private boolean isOutsideBusinessHours() {
		return false;
	}

}
