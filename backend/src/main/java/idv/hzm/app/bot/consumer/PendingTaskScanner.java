package idv.hzm.app.bot.consumer;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.RecordId;
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
    private static final long MAX_DELIVERY_COUNT = 3;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    private RequestConsumer requestConsumer;

    @Scheduled(fixedDelay = 60000)
    public void scanPendingTasks() {
        scanStream(RedisStreamConfig.REQUEST_STREAM, RedisStreamConfig.REQUEST_CONSUMER_GROUP);
    }

    private void scanStream(String streamKey, String consumerGroup) {
        logger.info("[RECOVERY] Scanning for pending tasks in stream: {}", streamKey);

        reactiveRedisTemplate.opsForStream()
                .pending(streamKey, consumerGroup)
                .flatMapMany(summary -> {
                    long totalPending = summary.getTotalPendingMessages();
                    if (totalPending == 0)
                        return Flux.<PendingMessage>empty();

                    logger.info("[RECOVERY] Found {} pending messages in {}", totalPending, streamKey);
                    return reactiveRedisTemplate.opsForStream()
                            .pending(streamKey, consumerGroup,
                                    org.springframework.data.domain.Range.unbounded(), 100)
                            .flatMapMany(Flux::fromIterable);
                })
                .filter(pendingMessage -> {
                    Duration idle = pendingMessage.getElapsedTimeSinceLastDelivery();
                    return idle != null && idle.compareTo(MIN_IDLE_TIME) > 0;
                })
                .flatMap(pm -> claimAndProcess(pm, streamKey, consumerGroup))
                .subscribe(
                        id -> logger.info("[RECOVERY] Successfully recovered message ID: {}", id),
                        error -> logger.error("[RECOVERY] Error during pending task scan: {}: {}",
                                error.getClass().getSimpleName(), error.getMessage()));
    }

    @SuppressWarnings("unchecked")
    private Mono<String> claimAndProcess(PendingMessage pendingMessage, String streamKey, String consumerGroup) {
        long deliveryCount = pendingMessage.getTotalDeliveryCount();
        String messageId = pendingMessage.getIdAsString();

        logger.warn("[RECOVERY] Claiming message: ID={}, IdleTime={}, deliveryCount={}, stream={}",
                messageId, pendingMessage.getElapsedTimeSinceLastDelivery(), deliveryCount, streamKey);

        // 超過最大重試次數，移至 DLQ
        if (deliveryCount > MAX_DELIVERY_COUNT) {
            logger.warn("[RECOVERY] Message {} exceeded max delivery count ({}), moving to DLQ",
                    messageId, MAX_DELIVERY_COUNT);
            return moveToDlqAndAck(pendingMessage, streamKey, consumerGroup);
        }

        // Claim 並重新處理
        return reactiveRedisTemplate.opsForStream()
                .claim(streamKey, consumerGroup, RECOVERY_CONSUMER, MIN_IDLE_TIME, pendingMessage.getId())
                .next()
                .flatMap(record -> {
                    MapRecord<String, String, String> typedRecord =
                            (MapRecord<String, String, String>) (MapRecord<?, ?, ?>) record;
                    logger.info("[RECOVERY] Reprocessing claimed message: {}", typedRecord.getId().getValue());
                    return requestConsumer.processRecord(typedRecord)
                            .thenReturn(typedRecord.getId().getValue());
                })
                .switchIfEmpty(Mono.fromRunnable(() ->
                    logger.debug("[RECOVERY] Message {} could not be claimed (already processed or missing)",
                            messageId)
                ).then(Mono.empty()));
    }

    private Mono<String> moveToDlqAndAck(PendingMessage pendingMessage, String streamKey, String consumerGroup) {
        String messageId = pendingMessage.getIdAsString();
        RecordId recordId = pendingMessage.getId();

        // 先讀取訊息內容
        return reactiveRedisTemplate.opsForStream()
                .claim(streamKey, consumerGroup, RECOVERY_CONSUMER, MIN_IDLE_TIME, recordId)
                .next()
                .flatMap(record -> {
                    @SuppressWarnings("unchecked")
                    MapRecord<String, String, String> typedRecord =
                            (MapRecord<String, String, String>) (MapRecord<?, ?, ?>) record;
                    Map<String, String> dlqValue = new java.util.HashMap<>(typedRecord.getValue());
                    dlqValue.put("error", "exceeded max delivery count");
                    dlqValue.put("originalId", messageId);
                    dlqValue.put("failedAt", String.valueOf(System.currentTimeMillis()));

                    return reactiveRedisTemplate.opsForStream()
                            .add(RedisStreamConfig.BOT_DLQ_STREAM, dlqValue)
                            .doOnNext(id -> logger.warn("[DLQ] Message moved to DLQ: originalId={}, dlqId={}",
                                    messageId, id))
                            .then(reactiveRedisTemplate.opsForStream()
                                    .acknowledge(streamKey, consumerGroup, recordId)
                                    .then())
                            .thenReturn(messageId);
                })
                .switchIfEmpty(Mono.fromRunnable(() ->
                    logger.debug("[RECOVERY] Message {} could not be claimed for DLQ (already gone)", messageId)
                ).then(Mono.empty()));
    }
}
