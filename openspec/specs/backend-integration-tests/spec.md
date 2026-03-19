## ADDED Requirements

### Requirement: Isolated Datastore Integration Testing
The Spring Boot test suite SHALL utilize Testcontainers to verify interactions with Redis and PostgreSQL inside isolated, ephemeral Docker containers.

#### Scenario: Verifying Redis Stream XACK routing
- **WHEN** the integration test executes the `BotConsumer` worker logic
- **THEN** it SHALL verify that correctly processed LLM chunks result in an `XACK` command, and failed processing routes the message to the DLQ (`stream:bot:dlq`).

### Requirement: Reactive Pipeline Verification
The test suite SHALL guarantee the temporal correctness of WebFlux components.

#### Scenario: Reconnection sequencing
- **WHEN** an SSE connection is initiated with a `Last-Event-ID`
- **THEN** `StepVerifier` SHALL assert that historical database chunks are emitted *before* any live Redis stream data is merged into the Flux.
