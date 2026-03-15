# Goal Description

The user wants to display reference materials (sources) when the chatbot provides an answer. By default, AnythingLLM returns a `sources` array the final Server-Sent Event (SSE) chunk (when `close: true`). We will modify the backend Java WebClient to parse and pass this `sources` array to the frontend via STOMP WebSocket, and then update the frontend UI to display these sources properly.

## Proposed Changes

### Backend

#### [MODIFY] backend/src/main/java/idv/hzm/app/common/dto/MessagePayload.java
- Add a new field `private List<Map<String, Object>> sources;` along with its getter/setter to allow transporting the sources JSON structure across the backend.

#### [MODIFY] backend/src/main/java/idv/hzm/app/bot/client/AnythingLLMClient.java
- Modify [chatStream](file:///Users/j/web/chatbot/LingBot/backend/src/main/java/idv/hzm/app/bot/client/AnythingLLMClient.java#41-70) return type from `Flux<String>` to `Flux<Map<String, Object>>` so it passes the raw parsed SSE chunk down the reactive pipeline instead of just extracting `textResponse`.
- Update [parseChunk](file:///Users/j/web/chatbot/LingBot/backend/src/main/java/idv/hzm/app/bot/client/AnythingLLMClient.java#71-89) to return `event` (the entire SSE chunk map) rather than just the string. Remove the `Flux.empty()` return from when `close` is true, so that the stream termination chunk can actually be processed in `doOnNext()`.

#### [MODIFY] backend/src/main/java/idv/hzm/app/bot/consumer/BotConsumer.java
- Update the [subscribe](file:///Users/j/web/chatbot/LingBot/backend/src/main/java/idv/hzm/app/bot/consumer/BotConsumer.java#38-54) and [onMessage](file:///Users/j/web/chatbot/LingBot/backend/src/main/java/idv/hzm/app/bot/consumer/BotConsumer.java#55-83) to handle `Flux<Map<String, Object>>`.
- Inside `doOnNext()`, if `event.get("close")` is `true`, construct a `STREAM_END` event, populate the `MessagePayload.sources` field with the JSON array, and publish it to the WebSocket channel via Redis.
- If it's a regular chunk, extract the `textResponse` and send it as a `STREAM_CHUNK` event.

### Frontend

#### [MODIFY] frontend/js/ChatApp.js
- In [_initSession()](file:///Users/j/web/chatbot/LingBot/frontend/js/ChatApp.js#22-45), pass the `sources` field array to `this.chatUI.finishStreamBubble(msg.payload?.sources);` when handling the `STREAM_END` message.

#### [MODIFY] frontend/js/ChatUI.js
- Modify [finishStreamBubble(sources)](file:///Users/j/web/chatbot/LingBot/frontend/js/ChatUI.js#114-119) to accept the `sources` array parameter.
- Implement an `appendSources(li, sources)` helper. It will append a new `<div class="sources-container">` containing the document titles inside the existing chat bubble `<li class="chat incoming">`.

#### [MODIFY] frontend/css/style.css
- Add CSS styling for `.sources-container` to render the references nicely with a slightly smaller font, subtle background hue, and nice padding within the bubble.

## Verification Plan

### Automated Tests
Since this is an interactive chatbot, we will verify this by using the browser subagent.

### Manual Verification
1. Open the Chatbot UI.
2. Send a query that requires AnythingLLM to query the workspace documents (e.g. "What is a VPN?" or "tell me about VPN").
3. Wait for the streaming response to print out.
4. Verify that once the string finishes, a "參考資料:" (Sources:) block appears at the bottom of the bot's newly generated text bubble.
