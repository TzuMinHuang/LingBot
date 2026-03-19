## Why

The test automation infrastructure (Playwright E2E, Testcontainers integration, k6 load tests) was implemented with several issues identified during code review: a race condition in `BotConsumerIntegrationTest` where the live `RequestConsumer` competes with the test for stream records, missing session cleanup in reactive tests, undocumented k6 SSE limitations, and missing `.gitignore` entries for Playwright build artifacts.

## What Changes

- **Fix race condition**: Disable `RequestConsumer` during integration tests by setting `app.consumer-count=0` via test properties, preventing the live consumer from stealing test records from `stream:request`.
- **Add test session cleanup**: Add `@AfterEach` cleanup in `SseSessionManagerTest` to remove orphaned drain threads.
- **Document k6 SSE limitation**: Add prominent comments in k6 scripts explaining that `http.get()` is not a true EventSource and documenting the `xk6-sse` alternative.
- **Fix `.gitignore`**: Add `playwright-report/` and `test-results/` entries for Playwright output artifacts.
- **Clarify Playwright config**: Add comment to `playwright.config.js` explaining the `baseURL` is only used as a fallback since all requests are intercepted via `page.route`.

## Capabilities

### New Capabilities
<!-- No new capabilities — this is a fix/cleanup change -->

### Modified Capabilities
<!-- No requirement-level changes to existing specs — these are implementation-quality fixes -->

## Impact

- **Backend test suite**: `BotConsumerIntegrationTest` and `SseSessionManagerTest` in `src/test/java`
- **Frontend test config**: `frontend/playwright.config.js`
- **Load test scripts**: `tests/load/stress_sse.js`, `tests/load/chaos_reconnect.js`
- **Repository config**: `.gitignore`
