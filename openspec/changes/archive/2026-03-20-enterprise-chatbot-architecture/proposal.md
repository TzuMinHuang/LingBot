## Why

The current web chatbot architecture is stateless and vulnerable to sudden disconnects, causing a poor user experience and potential loss of context in an enterprise environment. Transitioning to a Stateful, Event-driven architecture with Single Source of Truth ensures robust reconnections, seamless UI recovery, and secure access management via SSO, satisfying the high reliability demands of enterprise applications.

## What Changes

- **Implement Event Sourcing for UI**: All user interactions (messages, pause, cancel, FAQ clicks) will be recorded as replayable events.
- **Persistent Message Chunks**: LLM streaming chunks will be persisted (DB/Redis) to allow recovering and resuming a stream using cursor-based `Last-Event-ID`.
- **SSO Authentication Integration**: Introduce enterprise SSO to manage secure, identity-bound chat sessions.
- **Redefine PAUSE meaning**: PAUSE will now directly abort the LLM generation (similar to CANCEL) but will semantically indicate storing partial outputs without `RESUME` to simplify state management.
- **BREAKING**: Reconnect mechanics now require fetching historical Event Logs and Chunks before hooking into the live SSE stream, altering the frontend-backend connection handshake.

## Capabilities

### New Capabilities
- `sso-auth`: Handles JWT token generation, SSO integration (can be mocked for now), and session binding for employee identity.
- `stateful-chat-session`: Manages conversations, message states (STREAMING, PAUSED, CANCELLED), and cursor-based reconnect logistics to recover gracefully from disconnects.
- `event-sourcing-ui`: Captures UI-level actions such as button clicks, scrolling or pausing, mapping them into event payloads for perfect UI state reconstruction upon reconnect.

### Modified Capabilities
- `stream-reliability`: The requirement for streaming is upgraded to support explicit `CANCEL`/`PAUSE` interrupts that abort generation mid-stream and update the persisted status synchronously.

## Impact

- **Database Layers**: Introduces new schemas (Postgres) including `conversations`, `messages`, `message_chunks`, and `ui_events`.
- **API Endpoints**: Modified `/stream` to accept `Last-Event-ID` and handle history recovery.
- **LLM Worker**: Must integrate interrupt checks against Redis control flags to abort connections dynamically.
- **Frontend**: Must adapt to playback capabilities, fetching recent events to recreate UI state upon reload before normal interaction resumes.
