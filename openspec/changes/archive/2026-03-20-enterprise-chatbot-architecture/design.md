## Context

The current iteration of the web chatbot is robust in handling streaming pressure but lacks enterprise-level session persistence and UI state recovery. If a user closes their browser or switches devices, the conversation state is lost from the UI perspective, even if the LLM finishes the background task. To deploy this to enterprise employees, we need an architecture where every state (Chat, Streaming, UI) is recoverable from the backend database.

## Goals / Non-Goals

**Goals:**
- Provide 100% resurrectable chat sessions across devices using an Event Sourcing model for UI actions.
- Ensure streaming chunks are persisted (cursor-based) so a disconnected user can resume exactly where they left off.
- Support strict SSO session definitions for enterprise identity tracking.

**Non-Goals:**
- Complex multi-agent routing.
- Native LLM pause-and-resume prompting (we will use a fast-fail PAUSE that acts as a connection abort to save LLM tokens and ensure system predictability).

## Decisions

1. **Stateful Streaming with Chunks Persisted to DB**
   - *Rationale*: To handle reconnections flawlessly, we need a definitive `Last-Event-ID`. Persisting chunks to Postgres (or Redis Hash) guarantees that a reconnecting client immediately fetches missed data before listening to the live Stream. This trades slightly higher write IO for bulletproof reliability.
2. **Event Sourcing for UI Operations**
   - *Rationale*: Saving UI states (like expanding an FAQ or hitting Pause) as events (`ui_events` table) allows the frontend to replay exactly what the user was seeing. It provides a perfect audit log and solves the "multi-device sync" problem seamlessly.
3. **PAUSE equals Abort (No native RESUME)**
   - *Rationale*: Instead of storing partial prompts and keeping the LLM context alive (which is technically fragile across local LLMs like Ollama), PAUSE will abort the HTTP WebClient request to the LLM immediately. It is semantically treated as a "Stop Generation" command. The partial response is preserved, but no automatic continuation is attempted.

## Risks / Trade-offs

- [High Database IO] → Mitigation: If Postgres struggles with high concurrency chunk inserts, we will batch persist chunks or use Redis Sorted Sets as an intermediate buffer, flushing to PostgreSQL on `STREAM_END`.
- [Large Event Replay Payload] → Mitigation: In extremely long continuous sessions, replaying 10,000+ events might lag the browser. We will mitigate this by implementing UI state snapshotting at the `Message` boundaries in a future iteration.
