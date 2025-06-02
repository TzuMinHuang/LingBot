package idv.hzm.app.admin.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import idv.hzm.app.admin.dto.QueueInfoDto;
import idv.hzm.app.admin.service.QueueService;

@Service
public class ZsetQueueService implements QueueService {

	@Autowired
	private StringRedisTemplate redisTemplate;

	private static final String QUEUE_KEY = "queue:zset";

	@Override
	public QueueInfoDto isAlreadyInQueue(String sessionId) {
		Boolean exists = this.redisTemplate.opsForZSet().score(QUEUE_KEY, sessionId) != null;

		QueueInfoDto queueInfoDto = new QueueInfoDto();
		queueInfoDto.setSessionId(sessionId);
		queueInfoDto.setAlreadyInQueue(exists.booleanValue());
		return queueInfoDto;
	}

	@Override
	public QueueInfoDto enqueue(String sessionId) {
		// 嘗試加入，如果已存在則不會加入（NX：Only if not exists）
		Boolean added = this.redisTemplate.opsForZSet().addIfAbsent(QUEUE_KEY, sessionId, System.currentTimeMillis());
		if (Boolean.FALSE.equals(added)) {
			// 標示為「已在佇列」
		}

		QueueInfoDto queueInfoDto = new QueueInfoDto();
		queueInfoDto.setSessionId(sessionId);
		queueInfoDto.setAlreadyInQueue(true);
		return queueInfoDto;
	}

	@Override
	public QueueInfoDto cancelQueue(String sessionId) {
		boolean isAlreadyInQueue;
		Long removed = this.redisTemplate.opsForZSet().remove(QUEUE_KEY, sessionId);
		if (removed != null && removed > 0) {
			isAlreadyInQueue = false;
		} else {
			isAlreadyInQueue = true;
		}

		QueueInfoDto queueInfoDto = new QueueInfoDto();
		queueInfoDto.setSessionId(sessionId);
		queueInfoDto.setAlreadyInQueue(isAlreadyInQueue);
		return queueInfoDto;
	}

	@Override
	public QueueInfoDto dequeue(String agentId) {
		Set<String> result = this.redisTemplate.opsForZSet().range(QUEUE_KEY, 0, 0);
		if (result == null || result.isEmpty()) {
			return null;
		}

		String sessionId = result.iterator().next();
		this.redisTemplate.opsForZSet().remove(QUEUE_KEY, sessionId);

		QueueInfoDto queueInfoDto = new QueueInfoDto();
		queueInfoDto.setSessionId(sessionId);
		queueInfoDto.setAlreadyInQueue(false);
		return queueInfoDto;
	}

	@Override
	public List<QueueInfoDto> getQueueInfo() {
		Set<String> result = this.redisTemplate.opsForZSet().range(QUEUE_KEY, 0, -1);
		if (result == null) {
			return new ArrayList<>();
		}

		List<QueueInfoDto> queueInfoDtoList = new ArrayList<>();
		int index = 1;
		for (String sessionId : result) {
			QueueInfoDto queueInfoDto = new QueueInfoDto();
			queueInfoDto.setSessionId(sessionId);
			queueInfoDto.setAlreadyInQueue(true);
			queueInfoDto.setPosition(index++);
			queueInfoDtoList.add(queueInfoDto);
		}
		return queueInfoDtoList;
	}

}
