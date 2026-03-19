## ADDED Requirements

### Requirement: UI Interaction Event Log
All designated UI interactions that mutate the user's conversational context SHALL be transmitted to the backend and stored as immutable sequential events.

#### Scenario: User pauses generation
- **WHEN** the user clicks the "Pause" (Stop Generation) button
- **THEN** the UI SHALL emit a `PAUSE` event to the backend, which is recorded in the `ui_events` table and subsequently triggers the background Worker to abort the LLM connection.

#### Scenario: Reconstructing UI state from events
- **WHEN** a user loads a conversation on a fresh device
- **THEN** the frontend SHALL fetch the `ui_events` history and replay it locally to reconstitute identical UI bounds (e.g., expanded panels, disabled buttons) as their original device.
