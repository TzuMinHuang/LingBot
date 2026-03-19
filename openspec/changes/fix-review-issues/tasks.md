## 1. Fix Race Condition in Integration Tests

- [x] 1.1 Add `@TestPropertySource(properties = "app.consumer-count=0")` to `AbstractIntegrationTest` to disable `RequestConsumer` during all integration tests.

## 2. Add Test Session Cleanup

- [x] 2.1 Add `@AfterEach` method to `SseSessionManagerTest` that calls `sseSessionManager.remove()` for all session IDs used in tests.

## 3. Document k6 SSE Limitations

- [x] 3.1 Add prominent comment block at the top of `stress_sse.js` explaining that `http.get()` is not a true EventSource and documenting `xk6-sse` as the alternative for real SSE testing.
- [x] 3.2 Add the same limitation comment to `chaos_reconnect.js`.

## 4. Fix .gitignore and Config

- [x] 4.1 Add `playwright-report/` and `test-results/` to `.gitignore`.
- [x] 4.2 Add clarifying comment to `playwright.config.js` explaining that `baseURL` is a fallback since all network calls are intercepted via `page.route`.
