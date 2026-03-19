## ADDED Requirements

### Requirement: Integration tests operate in isolation from production consumers
Integration tests that exercise Redis Stream operations (XADD, XACK, DLQ) SHALL NOT compete with live consumer threads for stream records.

#### Scenario: RequestConsumer disabled during integration tests
- **WHEN** `BotConsumerIntegrationTest` executes in the Spring test context
- **THEN** no `RequestConsumer` instances SHALL be subscribed to `stream:request`, ensuring test records remain readable by the test's own `StepVerifier` assertions.

### Requirement: Test sessions are cleaned up after each test
Reactive stream tests that create `SseSessionManager` sessions SHALL release all associated resources (drain threads, sinks, queues) after each test method completes.

#### Scenario: No orphaned drain threads after test failure
- **WHEN** a `SseSessionManagerTest` method completes (pass or fail)
- **THEN** all sessions created during that test SHALL be removed via `SseSessionManager.remove()`.
