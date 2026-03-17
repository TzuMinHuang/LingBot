package idv.hzm.app.bot.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import idv.hzm.app.bot.config.SseEmitterManager;
import idv.hzm.app.bot.dto.EventDto;
import idv.hzm.app.bot.dto.QueuePayload;

@Component
public class QueueStatusPublisher {

	private static final Logger logger = LoggerFactory.getLogger(QueueStatusPublisher.class);

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private SseEmitterManager sseEmitterManager;

	@Autowired
	private RedisStreamConfig redisStreamConfig;

	@Autowired
	private ObjectMapper objectMapper;

	/**
	 * 每 5 秒廣播一次排隊狀態給所有連線中的 SSE 客戶端
	 */
//	@Scheduled(fixedRate = 5000)
	public void broadcastQueueStatus() {
		// 根據需求改為 API 主動查詢，暫停背景廣播
		/*
		if (sseEmitterManager.getConnectedSessions().isEmpty()) {
			return;
		}

		Long len = redisTemplate.opsForStream().size(RedisStreamConfig.BOT_INCOMING_STREAM);
		long streamLen = len != null ? len : 0L;
		int consumers = redisStreamConfig.getConsumerCount();

		// 取得正在處理中的訊息（Pending）
		// 注意：這裡簡單化處理，排隊位置 = (Stream 總長度 - 已消費但未 Ack 的數量)
		// 但對於 SSE 來說，我們直接推播給所有連線者
		
		int totalWait = Math.max(0, (int) (streamLen - consumers));

		for (String sessionId : sseEmitterManager.getConnectedSessions()) {
			// 只有當該 session 確實在排隊（或者我們想持續更新狀態）時才發送
			// 這裡直接發送，前端會判斷是否顯示
			QueuePayload payload = new QueuePayload(totalWait);
			EventDto event = new EventDto();
			event.setSessionId(sessionId);
			event.setType(EventDto.TYPE_QUEUE_UPDATE);
			event.setPayload(payload);

			try {
				String json = objectMapper.writeValueAsString(event);
				// 直接透過 Pub/Sub 廣播，讓所有實例都能轉發給自家的 SSE Emitter
				redisTemplate.convertAndSend(SseRedisSubscriber.CHANNEL_NAME, json);
			} catch (JsonProcessingException e) {
				logger.error("Failed to serialize queue update for session {}: {}", sessionId, e.getMessage());
			}
		}
		*/
	}
}
