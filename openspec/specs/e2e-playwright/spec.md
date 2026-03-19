## ADDED Requirements

### Requirement: Cross-Browser UI Interaction Validation
The testing infrastructure SHALL validate the widget's behavior across multiple viewports and rendering engines without requiring a backend server.

#### Scenario: Mobile widget injection and expansion
- **WHEN** the Playwright test suite loads the dummy host page in a mobile viewport (e.g., 375x812)
- **THEN** it SHALL verify the injection of the toggle button and the subsequent 100dvh iframe expansion upon click.

### Requirement: SSE Network Mocking
The E2E suite SHALL intercept network calls to validate the UI's reaction to simulated backend streaming events.

#### Scenario: Simulating a slow LLM stream
- **WHEN** the Playwright script intercepts the `/api/chat/stream` endpoint and emits chunks at 50ms intervals
- **THEN** the test SHALL verify that the typewriter effect renders correctly without queue backup, and the UI auto-scrolls successfully.
