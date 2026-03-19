## Context

The dual Redis Stream architecture (`stream:request` → `stream:response`) was recently introduced to replace the monolithic `BotConsumer` + Redis Pub/Sub pipeline. While the design direction is sound, the initial implementation has three critical reliability gaps:

1. **Thread leak**: `LlmThrottlingBuffer` creates a `ScheduledExecutorService` per request. If the LLM stream errors before `close:true`, the scheduler is never shut down.
2. **Message loss**: `ResponseConsumer` uses a single consumer group across nodes. Redis consumer groups partition messages — a response can be delivered to a node that doesn't hold the SSE session, where it's silently ACK'd and dropped.
3. **Dead recovery**: `PendingTaskScanner` claims timed-out messages but never reprocesses them, giving a false sense of reliability.

Additionally, there is no backpressure mechanism or operational visibility into the streaming pipeline.

## Goals / Non-Goals

**Goals:**
- Eliminate thread leaks in the throttling layer
- Guarantee every response message reaches the correct SSE session, even in multi-node deployments
- Make pending message recovery actually reprocess claimed messages
- Add bounded queues with overflow strategy to prevent OOM under load
- Expose key pipeline health metrics

**Non-Goals:**
- Changing the dual-stream architecture itself (request/response split stays)
- Frontend SSE client changes
- Horizontal auto-scaling logic
- Changing the LLM provider integration (`AnythingLLMClient`)

## Decisions

### D1: Shared ScheduledExecutorService for throttle timers

**Choice**: Replace per-instance `Executors.newSingleThreadScheduledExecutor()` in `LlmThrottlingBuffer` with a shared Spring-managed `ScheduledExecutorService` bean injected via constructor.

**Alternatives considered**:
- *Reactor `Flux.interval`*: Would avoid a thread pool entirely, but `LlmThrottlingBuffer` accumulates state in a `StringBuilder` with synchronized access — mixing Reactor schedulers with `synchronized` blocks risks deadlocks.
- *HashedWheelTimer (Netty)*: Efficient for many timers but adds a Netty dependency that isn't already in the reactive stack's transitive deps.

**Rationale**: A shared `ScheduledExecutorService` with 2-4 threads can serve hundreds of concurrent buffers (each just schedules a periodic check). Spring lifecycle handles shutdown. The buffer registers a repeating task on construction and cancels via `ScheduledFuture.cancel()` on `flushAndEnd()` or a new `close()` method called from error paths.

### D2: Per-node consumer group for ResponseConsumer

**Choice**: Each backend node creates its own consumer group on `stream:response` (e.g., `response-cg-<instanceId>`), so every node receives every message. Each node filters to local sessions and ACKs.

**Alternatives considered**:
- *Redis Pub/Sub fan-out*: Simpler broadcast but loses durability — if a node is temporarily down, messages are lost. The whole point of streams is durability.
- *Single group + NACK non-local messages*: Complex — NACKed messages go back to PEL and get reclaimed endlessly.
- *Topic per session*: Too many consumer groups; doesn't scale.

**Rationale**: Per-node groups give broadcast semantics with stream durability. Each node independently trims its read position. The `STREAM_MAXLEN` trim on `stream:response` prevents unbounded growth. Old consumer groups from terminated nodes are cleaned up by `RedisStreamInitializer` on startup (detect stale groups via `XINFO GROUPS`).

### D3: PendingTaskScanner reprocesses claimed messages

**Choice**: After claiming a message, feed it back through `RequestConsumer.processRecord()` directly.

**Alternatives considered**:
- *Remove scanner entirely*: Relies on consumer auto-retry, but Spring Data Redis `StreamReceiver` doesn't re-deliver pending messages automatically — they stay in PEL forever.
- *Move to DLQ after N retries*: Good addition but doesn't replace the need for initial reprocessing.

**Rationale**: The scanner already successfully claims messages. Adding a `processRecord()` call after claim completes the recovery loop. A `deliveryCount` check (from PEL metadata) will move messages to DLQ after 3 failed attempts to prevent infinite retry.

### D4: Bounded queue with drop-oldest overflow

**Choice**: `LlmThrottlingBuffer` uses an `ArrayBlockingQueue` (capacity configurable, default 512) instead of unbounded `StringBuilder` growth. On overflow, the oldest unflushed chunk is dropped and a warning is logged.

**Alternatives considered**:
- *Reject new chunks*: Could cause incomplete responses — worse UX than slightly delayed text.
- *Block producer*: Would block the LLM reactive stream, causing backpressure upstream to WebClient — risky for connection pool exhaustion.

**Rationale**: Drop-oldest is the safest overflow strategy for streaming text — the user sees slightly delayed text rather than a stalled or broken stream. In practice, overflow should be rare since the 50ms timer flush keeps the buffer small.

### D5: Actuator-based metrics

**Choice**: Expose metrics via Spring Boot Actuator + Micrometer gauges: `stream.request.lag`, `stream.response.lag`, `stream.request.pel.size`, `sse.active.sessions`, `consumer.idle.seconds`.

**Rationale**: Integrates with existing monitoring infrastructure (Prometheus/Grafana if configured). No new dependencies — Micrometer is already transitive via Spring Boot Actuator.

## Risks / Trade-offs

- **[Per-node consumer groups create N copies of reads]** → Acceptable for response stream (low volume, mostly text chunks). Mitigated by `STREAM_MAXLEN` trim.
- **[Stale consumer group cleanup on startup may race with active consumers]** → Use `XINFO GROUPS` + node heartbeat key in Redis. Only clean groups whose heartbeat key has expired (TTL 5 min).
- **[PendingTaskScanner reprocess may duplicate work if original consumer recovers late]** → Idempotency is already enforced via `interactionId` lock in `ChatRedisService`. Duplicate processing produces duplicate stream:response entries, but `ResponseConsumer` + `SseSessionManager` can deduplicate by `interactionId`.
- **[Shared scheduler for throttle timers is a single point of contention]** → With 2-4 threads and sub-millisecond tasks (just checking a buffer length), contention is negligible up to thousands of concurrent sessions.
