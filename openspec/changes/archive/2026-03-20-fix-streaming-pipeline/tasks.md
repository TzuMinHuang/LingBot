## 1. Fix Thread Leak in LlmThrottlingBuffer

- [x] 1.1 Add a shared `ScheduledExecutorService` bean (name: `throttleScheduler`, 2 threads) to `ThreadPoolConfig`
- [x] 1.2 Refactor `LlmThrottlingBuffer` constructor to accept the shared scheduler instead of creating its own; store `ScheduledFuture` reference from `scheduleAtFixedRate`
- [x] 1.3 Add `close()` method to `LlmThrottlingBuffer` that cancels the `ScheduledFuture` and flushes remaining buffer content
- [x] 1.4 Update `flushAndEnd()` to cancel via `ScheduledFuture.cancel()` instead of `scheduler.shutdown()`
- [x] 1.5 Update `RequestConsumer.processRecord()` error path (`onErrorResume`) to call `throttle.close()`
- [x] 1.6 Update `RequestConsumer` to inject the shared scheduler and pass it to each `LlmThrottlingBuffer` instance

## 2. Fix ResponseConsumer Consumer Group (Per-Node Broadcast)

- [x] 2.1 Generate a stable `INSTANCE_ID` in `ResponseConsumer` (or reuse existing) and create a per-node consumer group name: `response-cg-<instanceId>`
- [x] 2.2 Update `RedisStreamInitializer` to create the per-node consumer group on `stream:response` at startup
- [x] 2.3 Update `ResponseConsumer.subscribe()` to use the per-node consumer group instead of the shared `RESPONSE_CONSUMER_GROUP`
- [x] 2.4 Add node heartbeat key in Redis (`node:heartbeat:<instanceId>`, TTL 5 min) refreshed periodically
- [x] 2.5 Add stale consumer group cleanup in `RedisStreamInitializer`: on startup, `XINFO GROUPS` on `stream:response`, remove groups whose heartbeat key has expired
- [x] 2.6 Remove the static `RESPONSE_CONSUMER_GROUP` constant from `RedisStreamConfig` (or keep for backward compat during migration)

## 3. Fix PendingTaskScanner to Reprocess Claimed Messages

- [x] 3.1 Inject `RequestConsumer` into `PendingTaskScanner`
- [x] 3.2 After successful `XCLAIM`, read the claimed record's value and call `RequestConsumer.processRecord()`
- [x] 3.3 Add delivery count check: if `PendingMessage.getTotalDeliveryCount() > 3`, move to DLQ instead of reprocessing
- [x] 3.4 ACK the message after successful reprocessing or DLQ move

## 4. Add Backpressure Control

- [x] 4.1 Add `app.throttle-buffer-capacity` property to `application.yml` (default: 512)
- [x] 4.2 Refactor `LlmThrottlingBuffer` internal storage from `StringBuilder` to a bounded `ArrayBlockingQueue<String>` with configurable capacity
- [x] 4.3 Implement drop-oldest overflow: when queue is full, poll oldest entry, log WARN, then offer new chunk
- [x] 4.4 Update `flush()` to drain the queue into a single string for Redis write

## 5. Add Observability Metrics

- [x] 5.1 Create `StreamMetricsCollector` component that registers Micrometer gauges on startup
- [x] 5.2 Implement `stream.request.lag` gauge — periodic `XINFO GROUPS` on `stream:request` to read pending count
- [x] 5.3 Implement `stream.response.lag` gauge — same for `stream:response` (per-node group)
- [x] 5.4 Implement `stream.request.pel.size` gauge — PEL size from `XPENDING` summary
- [x] 5.5 Implement `sse.active.sessions` gauge — read from `SseSessionManager.getConnectedSessions().size()`
- [x] 5.6 Implement `consumer.idle.seconds` gauge — track last-processed timestamp per consumer name, compute delta

## 6. Cleanup

- [x] 6.1 Remove legacy constants (`BOT_INCOMING_STREAM`, `CONSUMER_GROUP`) from `RedisStreamConfig` if no remaining references
- [x] 6.2 Remove empty `QueueStatusPublisher` class if fully replaced by API endpoint
- [x] 6.3 Verify `RedisStreamInitializer` no longer creates old consumer groups for deleted streams
