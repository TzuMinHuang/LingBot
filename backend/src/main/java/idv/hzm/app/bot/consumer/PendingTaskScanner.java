package idv.hzm.app.bot.consumer;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import idv.hzm.app.bot.config.RedisStreamConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class PendingTaskScanner {

    private static final Logger logger = LoggerFactory.getLogger(PendingTaskScanner.class);
    private static final String RECOVERY_CONSUMER = "recovery-consumer";
    private static final Duration MIN_IDLE_TIME = Duration.ofMinutes(1);

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Scheduled(fixedDelay = 60000) // 每一分鐘掃描一次
    public void scanPendingTasks() {
        logger.info("[RECOVERY] Scanning for pending tasks in stream: {}", RedisStreamConfig.BOT_INCOMING_STREAM);

        reactiveRedisTemplate.opsForStream()
                .pending(RedisStreamConfig.BOT_INCOMING_STREAM, RedisStreamConfig.CONSUMER_GROUP)
                .flatMapMany(summary -> {
                    long totalPending = summary.getTotalPendingMessages();
                    if (totalPending == 0) return Flux.<PendingMessage>empty();
                    
                    logger.info("[RECOVERY] Found {} pending messages", totalPending);
                    // 取得詳細的待處理訊息清單
                    return reactiveRedisTemplate.opsForStream()
                            .pending(RedisStreamConfig.BOT_INCOMING_STREAM, RedisStreamConfig.CONSUMER_GROUP, org.springframework.data.domain.Range.unbounded(), 100);
                })
                .cast(PendingMessage.class)
                .filter(pendingMessage -> {
                    Duration idle = pendingMessage.getElapsedTimeSinceLastDelivery();
                    return idle != null && idle.compareTo(MIN_IDLE_TIME) > 0;
                })
                .flatMap(this::claimMessage)
                .subscribe(
                    claimedId -> logger.info("[RECOVERY] Successfully claimed message ID: {}", claimedId),
                    error -> logger.error("[RECOVERY] Error during pending task scan: {}", error.getMessage())
                );
    }

    private Mono<String> claimMessage(PendingMessage pendingMessage) {
        logger.warn("[RECOVERY] Claiming timed-out message: ID={}, IdleTime={}", 
                pendingMessage.getIdAsString(), pendingMessage.getElapsedTimeSinceLastDelivery());

        // 使用 XCLAIM 將訊息轉發給 recovery-consumer 重新觸發 BotConsumer 的處理流程
        // 注意：這裡簡單地重新分配，BotConsumer 的不同實例會再次收到此訊息（因為分配給了群組中的一個名稱）
        return reactiveRedisTemplate.opsForStream()
                .claim(RedisStreamConfig.BOT_INCOMING_STREAM, 
                       RedisStreamConfig.CONSUMER_GROUP, 
                       RECOVERY_CONSUMER, 
                       MIN_IDLE_TIME, 
                       pendingMessage.getId())
                .next()
                .map(record -> record.getId().getValue());
    }
}
