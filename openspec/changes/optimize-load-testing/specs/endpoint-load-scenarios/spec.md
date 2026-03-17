## ADDED Requirements

### Requirement: Chat Message Load Scenario
The system SHALL be able to simulate multiple concurrent users sending messages to the `/chat/{sessionId}/send` endpoint.

#### Scenario: Sustained message load
- **WHEN** 50 virtual users send messages every 2 seconds for a duration of 5 minutes
- **THEN** the system SHALL record success rates and P95 latency for the message enqueuing process.

### Requirement: Status Polling Load Scenario
The system SHALL support high-frequency polling of the `/chat/{sessionId}/queue-position` endpoint to simulate user waiting behavior.

#### Scenario: Intense queue polling
- **WHEN** 200 virtual users poll the queue position endpoint simultaneously
- **THEN** the backend SHALL remain responsive with a P99 latency lower than 200ms for status checks.
