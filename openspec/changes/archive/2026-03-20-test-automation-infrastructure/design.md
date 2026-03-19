## Context

The chatbot system currently operates with a high-concurrency architecture (Spring Boot + Redis + WebFlux SSE) and a vanilla JavaScript frontend widget. As the system scales toward enterprise features like SSO, UI Event Sourcing, and conversational state recovery, manual validation has become a bottleneck. We need a robust testing infrastructure that can validate end-to-end flows, backend integration points (especially Redis Streams and PostgreSQL), and system resilience under load without altering the core production code.

## Goals / Non-Goals

**Goals:**
- Establish a reproducible End-to-End (E2E) testing framework that simulates real browser interactions with the injected Chatbot widget.
- Implement integration tests that verify actual database and Redis behaviors via isolated containers.
- Provide a load testing baseline to validate SSE connection limits and recovery mechanisms under duress.

**Non-Goals:**
- Converting the vanilla JS frontend to a framework (React/Vue) solely to support unit testing; we will rely entirely on E2E testing for the frontend.
- Testing the actual AnythingLLM provider's AI response quality (we will mock the LLM responses to ensure determinism).

## Decisions

1. **Frontend E2E: Playwright**
   - *Rationale*: Since the frontend is a no-build Vanilla JS widget injected via an iframe, traditional JS unit testing (like Jest) provides minimal value. Playwright allows us to test the actual browser rendering, iframe manipulation, and network interceptions (e.g., mocking the SSE `/stream` endpoint) identically to how users experience it.
2. **Backend Integration: Testcontainers + StepVerifier**
   - *Rationale*: Redis Stream groups (`XREADGROUP`) and database chunking (`Last-Event-ID`) are highly stateful. Mocking these components often hides race conditions. `Testcontainers` spinning up real Redis and Postgres instances ensures our tests reflect production reality. `StepVerifier` handles the temporal testing of the WebFlux SSE endpoints elegantly.
3. **Load Testing: k6**
   - *Rationale*: Open-source, JavaScript-scriptable, and specifically capable of handling long-lived HTTP requests like Server-Sent Events (SSE). It flawlessly simulates thousand-user connection spikes and connection drops for chaos testing.

## Risks / Trade-offs

- [Slow CI Execution Time] → Mitigation: Testcontainers and Playwright E2E tests are inherently slower than unit tests. We will separate tests into `@Tag("fast")` and `@Tag("slow")` profiles, running full integration and E2E suites only on pre-merge or nightly builds.
- [Flaky Tests in E2E] → Mitigation: Playwright's auto-wait mechanisms will be strictly adhered to. We will rely on explicit data-attributes for querying rather than brittle CSS pathing.
