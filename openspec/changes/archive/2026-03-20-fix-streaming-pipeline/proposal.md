## Why

The recent refactor to a dual Redis Stream architecture (`stream:request` → `stream:response`) introduced several reliability issues: `LlmThrottlingBuffer` leaks a `ScheduledExecutorService` per request, `ResponseConsumer` uses a shared consumer group that silently drops messages in multi-node deployments, and `PendingTaskScanner` claims timed-out messages without reprocessing them. These bugs will cause thread exhaustion under load and message loss in production.

## What Changes

- **Replace per-request scheduler in `LlmThrottlingBuffer`** with a shared `ScheduledExecutorService` managed by Spring lifecycle, eliminating thread leaks on error paths.
- **Fix `ResponseConsumer` consumer group semantics** so every backend node receives every response message (per-node consumer groups or pub/sub fan-out), preventing silent message drops.
- **Fix `PendingTaskScanner`** to actually reprocess claimed messages by feeding them back through `RequestConsumer.processRecord`, or remove the scanner if consumer-level retry is sufficient.
- **Add backpressure controls** — bounded queues in the throttling buffer with a defined overflow strategy (drop-oldest or reject).
- **Add observability metrics** — expose stream lag, PEL size, and consumer idle time for monitoring.

## Capabilities

### New Capabilities
- `backpressure-control`: Bounded queue and overflow strategy for the LLM throttling buffer to prevent unbounded memory growth.
- `streaming-observability`: Metrics endpoints exposing Redis Stream lag, PEL size, consumer idle time, and active SSE session count.

### Modified Capabilities
- `stream-reliability`: Fix thread lifecycle management in `LlmThrottlingBuffer`, fix consumer group broadcast semantics in `ResponseConsumer`, and fix claim-without-reprocess in `PendingTaskScanner`.

## Impact

- **Code**: `LlmThrottlingBuffer`, `ResponseConsumer`, `PendingTaskScanner`, `ThreadPoolConfig`, `RedisStreamConfig`
- **Runtime**: Thread pool sizing changes; new shared scheduler bean
- **Ops**: New metrics available for monitoring dashboards
- **Risk**: Consumer group restructuring requires coordinated Redis Stream group recreation during deployment
