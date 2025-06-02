package idv.hzm.app.bot.flow.plugs.singleturn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import idv.hzm.app.bot.client.QueueClient;
import idv.hzm.app.bot.flow.core.ProcessContext;
import idv.hzm.app.bot.flow.core.StepStrategy;
import idv.hzm.app.bot.service.RespondUserService;

@Service
public class CancelTransferStep implements StepStrategy {

	@Autowired
	private QueueClient queueClient;
	@Autowired
	private RespondUserService respondUserService;

	@Override
	public boolean handle(ProcessContext processContext) {
		String sessionId = processContext.getSessionId();
		// 查看Redis內是否已在排隊
		if (this.queueClient.getStatus(sessionId)) {
			// 取消連線排隊，存入 Redis 表格內的訊息刪除
			this.queueClient.cancelQueue(sessionId);
			this.respondUserService.respondToUserWithText(sessionId, "已取消目前的排隊");
			return true;
		}
		this.respondUserService.respondToUserWithText(sessionId, "您目前未有排隊任務，有需要可以輸入轉接客服");
		return true;
	}

}
