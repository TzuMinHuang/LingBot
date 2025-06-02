package idv.hzm.app.admin.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import idv.hzm.app.admin.dto.QueueInfoDto;
import idv.hzm.app.admin.service.impl.ZsetQueueService;
import idv.hzm.app.common.api.CommonResult;

@RestController
@RequestMapping("/queue")
public class QueueController {

	@Autowired
	private ZsetQueueService queueService;

	// 加入排隊（enqueue）
	@PostMapping("/{sessionId}/enqueue")
	public CommonResult<QueueInfoDto> enqueue(@PathVariable String sessionId) {
		return CommonResult.success(this.queueService.enqueue(sessionId));
	}

	// 取消排隊（cancelQueue）
	@PostMapping("/{sessionId}/cancel")
	public CommonResult<QueueInfoDto> cancelQueue(@PathVariable String sessionId) {
		return CommonResult.success(this.queueService.cancelQueue(sessionId));
	}

	// 取出排隊者（dequeue，通常由客服端呼叫）
	@PostMapping("/{agentId}/dequeue")
	public CommonResult<QueueInfoDto> dequeue(@PathVariable String agentId) {
		return CommonResult.success(this.queueService.dequeue(agentId));
	}

	// 查詢某位用戶是否已在排隊
	@GetMapping("/{sessionId}/status")
	public CommonResult<QueueInfoDto> isAlreadyInQueue(@PathVariable String sessionId) {
		return CommonResult.success(this.queueService.isAlreadyInQueue(sessionId));
	}

	// 取得整個排隊資訊列表
	@GetMapping("/info")
	public CommonResult<List<QueueInfoDto>> getQueueInfo() {
		return CommonResult.success(this.queueService.getQueueInfo());
	}
}
