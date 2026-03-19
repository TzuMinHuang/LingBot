## 1. Frontend E2E Testing (Playwright)

- [ ] 1.1 Initialize standard `package.json` in `/frontend` directory exclusively for dev dependencies.
- [ ] 1.2 Install `@playwright/test` and generate standard Playwright configuration.
- [ ] 1.3 Write `widget-injection.spec.js` to assert iframe mounts correctly on a dummy host DOM.
- [ ] 1.4 Write `mobile-fullscreen.spec.js` to simulate 375x812 viewport and assert fullscreen behavior.
- [ ] 1.5 Write `sse-mock.spec.js` using `page.route` to mock `/api/chat/stream` and assert typewriter UI auto-scroll.

## 2. Backend Integration Testing (Testcontainers)

- [ ] 2.1 Add `testcontainers` and `postgresql` dependencies to Maven `pom.xml`.
- [ ] 2.2 Create `AbstractIntegrationTest.java` configuring dynamic properties for ephemeral PostgreSQL and Redis containers.
- [ ] 2.3 Write `BotConsumerIntegrationTest.java` asserting `XADD` execution, `XACK` confirmation, and DLQ routing inside the isolated Redis instance.

## 3. Reactive Stream Testing (Reactor)

- [ ] 3.1 Add `reactor-test` dependency to `pom.xml`.
- [ ] 3.2 Write `SseSessionManagerTest.java` using `StepVerifier` to assert `Last-Event-ID` historical DB chunk recovery sequence.

## 4. Load & Chaos Testing (k6)

- [ ] 4.1 Initialize a `/tests/load` directory.
- [ ] 4.2 Write `stress_sse.js` (k6 script) to simulate 1000 concurrent EventSource connections ramping up over 60 seconds.
- [ ] 4.3 Write `chaos_reconnect.js` to simulate unpredictable user network drops and instant `Last-Event-ID` reconnections.
