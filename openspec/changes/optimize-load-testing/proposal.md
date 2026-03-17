## Why

Currently, the chatbot system lacks a systematic way to measure performance and stability under pressure. Manual testing is insufficient for identifying bottlenecks in the decoupled asynchronous LLM pipeline (Redis Streams, Workers, etc.). Automated load testing is required to ensure the system can handle production-level traffic and to provide quantifiable data for architectural decisions.

## What Changes

- **Automated Load Testing Framework**: Integrate [k6](https://k6.io/) as the core load testing engine for the project.
- **Scenario Automation**: Implement automated script scenarios for core reactive endpoints (`/chat/{sessionId}/send`, `/chat/{sessionId}/queue-position`, etc.).
- **Report Generation**: Introduce an automated reporting pipeline that converts k6 results into human-readable HTML reports and structured JSON data for historical tracking.
- **Concurrency Parameterization**: Support configurable stress levels (VUs - Virtual Users) and durations to simulate various real-world loads.

## Capabilities

### New Capabilities
- `load-testing-core`: Base k6 infrastructure, shared utilities, and environment configuration.
- `endpoint-load-scenarios`: Specific scripts for benchmarking the chat, history, and status endpoints.
- `performance-reporting-pipeline`: Automation to generate, format, and archive performance metrics and visual reports.

### Modified Capabilities
<!-- No existing requirement-level capabilities are being modified. -->

## Impact

- **Developer Experience**: Provides a one-command way for developers to verify performance impact before PR submission.
- **Environment**: Performance tests may require a dedicated Redis/AnythingLLM staging environment to avoid data pollution.
- **Dependencies**: Adds a requirement for `k6` (can be run via Docker or binary).
