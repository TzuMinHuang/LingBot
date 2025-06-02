package idv.hzm.app.admin.service.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import idv.hzm.app.admin.dto.QueueInfoDto;
import idv.hzm.app.admin.entity.Pair;
import idv.hzm.app.admin.repo.PairRepository;
import idv.hzm.app.admin.service.QueueService;

@Service
public class LuaQueueService implements QueueService {

	@Autowired
	private StringRedisTemplate redisTemplate;
	@Autowired
	private PairRepository pairRepository;

	private static final String CUSTOMER_QUEUE_SET = "customer_queue_set";
	private static final String CUSTOMER_QUEUE = "customer_queue";

	@Override
	public QueueInfoDto isAlreadyInQueue(String sessionId) {
		Set<String> customerQueueSet = this.redisTemplate.opsForSet().members(CUSTOMER_QUEUE_SET);

		QueueInfoDto queueInfoDto = new QueueInfoDto();
		queueInfoDto.setSessionId(sessionId);
		if (customerQueueSet.contains(sessionId)) {
			queueInfoDto.setAlreadyInQueue(true);
		} else {
			queueInfoDto.setAlreadyInQueue(false);
		}
		return queueInfoDto;
	}

	@Override
	public QueueInfoDto enqueue(String sessionId) {

		// KEYS[1] = customer_queue_set,
		// KEYS[2] = customer_queue
		// ARGV[1] = userId
		String luaScript = """
				if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 0 then
				   redis.call('SADD', KEYS[1], ARGV[1]);
				   redis.call('RPUSH', KEYS[2], ARGV[1]);
				   return 1;
				else
				   return 0;
				end
				  """;
		DefaultRedisScript<Long> script = new DefaultRedisScript<>();
		script.setScriptText(luaScript);
		script.setResultType(Long.class);

		QueueInfoDto queueInfoDto = new QueueInfoDto();
		Long result = this.redisTemplate.execute(script, Arrays.asList(CUSTOMER_QUEUE_SET, CUSTOMER_QUEUE), sessionId);
		if (result != null && result == 1L) {
			queueInfoDto.setSessionId(sessionId);
		}
		return queueInfoDto;
	}

	@Override
	public QueueInfoDto cancelQueue(String sessionId) {
		String luaScript = """
				    local removedFromList = redis.call('LREM', KEYS[1], 0, ARGV[1]);
				    local removedFromSet = redis.call('SREM', KEYS[2], ARGV[1]);
				    return {removedFromList, removedFromSet};
				""";

		List<String> keys = Arrays.asList(CUSTOMER_QUEUE, CUSTOMER_QUEUE_SET);

		RedisScript<List<Long>> script = RedisScript.of(luaScript);

		List<Long> result = (List<Long>) redisTemplate.execute(script, keys, sessionId);

		System.out.printf("cancelOrder: removedFromList=%d, removedFromSet=%d%n", result.get(0), result.get(1));
		return null;
	}

	@Override
	public QueueInfoDto dequeue(String agentId) {
		String luaScript = """
				local userId = redis.call('LPOP', KEYS[1]);
				if userId then
				    if redis.call('SISMEMBER', KEYS[2], userId) == 1 then
				       redis.call('SREM', KEYS[2], userId);
				       redis.call('SET', KEYS[3], 'busy');
				       return userId;
				    else
				       return nil;
				    end
				else
				    return nil;
				end
				""";

		String userId = (String) this.redisTemplate.execute(RedisScript.of(luaScript, String.class),
				Arrays.asList(CUSTOMER_QUEUE, CUSTOMER_QUEUE_SET, "agent_status:" + agentId), agentId);

		if (userId != null) {
			try {
				this.pairRepository.save(new Pair(agentId, userId));
				// return Optional.of(userId);
			} catch (Exception e) {
				// 補救：將 userId 放回隊列末尾
				luaScript = """
						   redis.call('RPUSH', KEYS[1], ARGV[1]);
						   redis.call('SADD', KEYS[2], ARGV[1]);
						   redis.call('SET', KEYS[3], 'available');  -- 恢復客服空閒狀態
						""";
				this.redisTemplate.execute(RedisScript.of(luaScript, String.class),
						Arrays.asList(CUSTOMER_QUEUE, CUSTOMER_QUEUE_SET, "agent_status:" + agentId), agentId);
				System.out.printf("接單寫 DB 失敗，已回補用戶排隊：{}", userId, e.toString());
			}
		}
		return null;
	}

	@Override
	public List<QueueInfoDto> getQueueInfo() {
		return null;
	}

}
