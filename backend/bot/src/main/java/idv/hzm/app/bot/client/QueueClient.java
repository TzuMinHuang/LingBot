package idv.hzm.app.bot.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import idv.hzm.app.bot.client.dto.QueueInfoDto;
import idv.hzm.app.common.api.CommonResult;
import idv.hzm.app.common.api.ResultCode;
import idv.hzm.app.common.util.Json2Util;

@Component
public class QueueClient {

	@Value("${admin.base-url}")
	private String BASE_URL;

	@Autowired
	private RestTemplate restTemplate;

	public QueueInfoDto enqueue(String sessionId) {
		String url = String.format("%s/%s/enqueue", BASE_URL+"/admin/queue", sessionId);
		CommonResult<?> result = this.restTemplate.postForObject(url, null, CommonResult.class);
		return extractData(result, QueueInfoDto.class);
	}

	public QueueInfoDto cancelQueue(String sessionId) {
		String url = String.format("%s/%s/cancel", BASE_URL+"/admin/queue", sessionId);
		CommonResult<?> result = this.restTemplate.postForObject(url, null, CommonResult.class);
		return extractData(result, QueueInfoDto.class);
	}

	public QueueInfoDto dequeue(String agentId) {
		String url = String.format("%s/%s/dequeue", BASE_URL+"/admin/queue", agentId);
		CommonResult<?> result = this.restTemplate.postForObject(url, null, CommonResult.class);
		return extractData(result, QueueInfoDto.class);
	}

	public boolean getStatus(String sessionId) {
		String url = String.format("%s/%s/status", BASE_URL+"/admin/queue", sessionId);
		CommonResult<?> result = this.restTemplate.getForObject(url, CommonResult.class);
		QueueInfoDto queueInfoDto = extractData(result, QueueInfoDto.class);
		return queueInfoDto.isAlreadyInQueue();
	}

	public List<QueueInfoDto> getQueueInfo() {
		String url = String.format("%s/info", BASE_URL+"/admin/queue");
		CommonResult<?> result = this.restTemplate.getForObject(url, CommonResult.class);
		return extractList(result, QueueInfoDto.class);
	}

	// 這些方法需你自訂 JSON 對應轉換
	private <T> T extractData(CommonResult<?> result, Class<T> clazz) {
		if (ResultCode.SUCCESS.getCode() != result.getCode()) {
			throw new RuntimeException("請求排隊功能失敗");
		}
		return Json2Util.extractData(result.getData(), clazz);
	}

	private <T> List<T> extractList(CommonResult<?> result, Class<T> clazz) {

		if (ResultCode.SUCCESS.getCode() != result.getCode()) {
			throw new RuntimeException("請求排隊功能失敗");
		}
		return Json2Util.extractList(result.getData(), clazz);
	}
}
