## ADDED Requirements

### Requirement: Edge-to-Edge Mobile Viewport
On mobile devices (`max-width: 600px`), the chatbot iframe SHALL scale to exactly fill the visible screen without OS UI interference.

#### Scenario: Mobile widget expansion
- **WHEN** a user taps the toggle button on a mobile device
- **THEN** the iframe SHALL expand to `100vw` by `100dvh` and anchor directly to `bottom: 0, right: 0`, completely hiding the host website.

### Requirement: Seamless Mobile Toggle Disappearance
The external widget launch button SHALL NOT overlap or interfere with the fullscreen chatbot layout.

#### Scenario: Hiding the toggle button
- **WHEN** the iframe enters the open state on a mobile device
- **THEN** the external toggle button SHALL transition to `opacity: 0` and `pointer-events: none`.

### Requirement: Internal Dismissal Control
When the external toggle is hidden, the user MUST be able to dismiss the widget from within the iframe.

#### Scenario: Closing from inside the iframe
- **WHEN** a user clicks the "Close" button located in the `chatbot.html` header
- **THEN** the iframe SHALL emit a `postMessage` with `{ type: 'chatbot-close' }` to the parent `.js` script, causing the widget to collapse and the external toggle button to reappear.
