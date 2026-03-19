## 1. Database and Schema Updates

- [x] 1.1 Create `conversations`, `messages`, and `message_chunks` R2DBC repository interfaces
- [x] 1.2 Create `ui_events` table repository for Event Sourcing
- [x] 1.3 Add corresponding entity classes (Conversation, Message, MessageChunk, UiEvent)

## 2. SSO Authentication

- [x] 2.1 Set up mock enterprise SSO endpoint and JWT issuing filter
- [x] 2.2 Update API Gateway and controllers to extract and bind `user_id` to session requests

## 3. UI Event Sourcing

- [x] 3.1 Create a REST endpoint `POST /api/chat/events` to ingest frontend UI events
- [x] 3.2 Implement service logic to persist received `ui_events` mapped to `session_id`

## 4. Reconnect and Stateful Streaming

- [x] 4.1 Update `SseSessionManager` to accept and process `Last-Event-ID` from the frontend
- [x] 4.2 Implement query logic in `MessageChunkRepository` to fetch missed chunks
- [x] 4.3 Update `GET /api/chat/stream/{conversationId}` to orchestrate the combined history and live Flux

## 5. Streaming Persistence & Interrupts

- [x] 5.1 Modify `RequestConsumer` and `LlmThrottlingBuffer` to accurately persist chunks to `message_chunks` table continuously
- [x] 5.2 Implement control signal checking (PAUSE/CANCEL) in the background worker processing loop to abort LLM calls
- [x] 5.3 Update `LlmThrottlingBuffer` to properly close ScheduledFutures when an intentional abort occurs
