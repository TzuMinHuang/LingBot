## ADDED Requirements

### Requirement: Cursor-Based Streaming Recovery
The backend SHALL persist every LLM chunk generated during a conversation and allow clients to recover missed chunks via `Last-Event-ID`.

#### Scenario: Client reconnects mid-generation
- **WHEN** the frontend re-establishes an SSE connection providing a valid `Last-Event-ID` header
- **THEN** the `SseSessionManager` SHALL query the database for all chunks in the requested message with an index strictly greater than `Last-Event-ID`, emit them sequentially, and immediately transition to emitting live Redis stream chunks for the active message.

### Requirement: Database-Backed Conversational State
Every conversation and its individual messages SHALL maintain a strictly queryable status (`STREAMING`, `PAUSED`, `CANCELLED`, `COMPLETED`).

#### Scenario: Querying conversation history
- **WHEN** a user navigates to an existing conversation
- **THEN** the system SHALL return the full array of messages including their exact termination states, allowing the UI to render the accurate final state without reopening the SSE connection unless a message is actively `STREAMING`.
