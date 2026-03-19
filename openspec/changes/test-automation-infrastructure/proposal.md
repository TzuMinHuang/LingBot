## Why

As the chatbot architecture evolves into a sophisticated system combining Redis Streams, long-lived SSE connections, database-backed UI event sourcing, and a fluid mobile widget, manual testing is no longer viable. A comprehensive, automated testing infrastructure is urgently needed to prevent regressions, ensure data consistency across reconnections, and validate the UI's resilience under high concurrency before enterprise deployment.

## What Changes

- **Implement E2E UI Testing**: Integrate Playwright to automate tests for the vanilla JS widget, simulating viewport changes, iframe injection, and mocking SSE network connections.
- **Implement Backend Integration Testing**: Introduce `Testcontainers` (Redis, PostgreSQL) to the Spring Boot test suite, verifying actual `XADD`/`XREADGROUP` behavior and data persistence instead of relying solely on mocks.
- **Implement Reactive Stream Testing**: Utilize Reactor's `StepVerifier` to validate the temporal behavior of the SSE `/stream` endpoint (historical chunk recovery followed by live stream).
- **Implement Load & Chaos Testing**: Adopt `k6` to programmatically stress-test the SSE connections and simulate random connection drops to evaluate the system's fault tolerance and recovery mechanisms.

## Capabilities

### New Capabilities
- `e2e-playwright`: Defines the automated UI testing infrastructure for the vanilla JS frontend widget, including cross-device simulation and network interception.
- `backend-integration-tests`: Establishes the real-dependency testing framework for Spring Boot using Testcontainers and Reactor Test for the asynchronous pipelines.
- `load-resilience-testing`: Defines the performance evaluation benchmark and Chaos engineering criteria using k6 to validate SSE horizontal scalability.

### Modified Capabilities
<!-- No requirement changes to existing specs. Testing only augments the guarantee of existing functionality. -->

## Impact

- **Frontend**: Adds a `package.json` and a `tests/e2e` directory purely for Playwright (does not change the no-build delivery of the production widget).
- **Backend Test Suite**: Adds Docker requirements for local testing (Testcontainers) and introduces new dependencies (`reactor-test`, `testcontainers`).
- **DevOps/CI**: Provides actionable scripts that must be integrated into the CI/CD pipeline blocking PR merges on failure.
