## Context

The test automation infrastructure was recently implemented across 4 layers (Playwright, Testcontainers, StepVerifier, k6). A code review identified 5 issues ranging from a flaky-test-causing race condition to missing gitignore entries. All fixes are localized to test infrastructure — no production code changes.

## Goals / Non-Goals

**Goals:**
- Eliminate the `RequestConsumer` race condition that can cause `BotConsumerIntegrationTest` to intermittently fail
- Ensure all test sessions are properly cleaned up to avoid thread leaks
- Make k6 script limitations transparent to future users
- Keep Playwright artifacts out of version control

**Non-Goals:**
- Rewriting k6 scripts to use `xk6-sse` (requires custom k6 binary build — separate effort)
- Adding new test coverage — this is purely fixing existing test quality

## Decisions

1. **Disable RequestConsumer via `app.consumer-count=0` in test properties**
   - *Rationale*: The `RequestConsumer.subscribe()` loop is already guarded by `for (i = 1; i <= count; i++)`, so setting count to 0 cleanly prevents any consumer from starting. This is simpler than `@MockBean` (which replaces the entire bean and may cause dependency issues with `AnythingLLMClient`, `ChatHistoryService`, etc.) and more targeted than a test profile.
   - *Alternative considered*: Using a separate stream key for tests — rejected because it wouldn't test the actual production stream configuration.

2. **`@AfterEach` cleanup in SseSessionManagerTest**
   - *Rationale*: The `drainQueue` thread runs until the session is removed or the thread is interrupted. Tests using `flux.take(N)` trigger `doOnCancel → remove()`, but if the test fails mid-stream, orphaned threads accumulate. Explicit cleanup is defensive.

3. **Documentation-only fix for k6 SSE limitation**
   - *Rationale*: Migrating to `xk6-sse` requires building a custom k6 binary with Go extensions, which is a separate infrastructure decision. For now, clear documentation prevents misinterpretation of test results.

## Risks / Trade-offs

- [Setting consumer-count=0 disables all request consumers in integration tests] → This is intentional. `BotConsumerIntegrationTest` tests Redis stream *operations* (XADD/XACK/DLQ), not the full consumer pipeline. Full pipeline testing requires a separate test class with consumers enabled and mocked LLM responses.
