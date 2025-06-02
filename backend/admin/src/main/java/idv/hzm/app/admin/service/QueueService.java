package idv.hzm.app.admin.service;

import java.util.List;

import idv.hzm.app.admin.dto.QueueInfoDto;

public interface QueueService {

	public QueueInfoDto isAlreadyInQueue(String sessionId);

	public QueueInfoDto enqueue(String sessionId);

	public QueueInfoDto cancelQueue(String sessionId);

	public QueueInfoDto dequeue(String agentId);

	public List<QueueInfoDto> getQueueInfo();

}
