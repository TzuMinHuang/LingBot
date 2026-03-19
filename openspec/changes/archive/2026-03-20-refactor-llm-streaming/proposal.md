## Why

目前系統使用 Pub/Sub (`ws-channel`) 與為每位使用者獨立開立的 Redis Stream 進行 LLM 訊號傳遞，除了在 1000 人高併發時會引發 Redis 與網路 I/O 風暴外，並行的 WebFlux SSE 推送也易引發亂序或重複發送等 Bug。
我們需要將架構全面收斂至「僅兩條 Stream (request/response)」，並加入雙層節流 (Throttling) 與單線程 User Queue 以穩定承載海量併發，同時賦予系統未來輕易擴展至多 Backend 節點的能力。

## What Changes

- **BREAKING**: 移除原有的 `ws-channel` Pub/Sub 機制以及動態產生的 `stream:chat:res:<sessionId>`。
- 建立只依賴 `stream:request` 與 `stream:response` 的雙 Stream 收發架構。
- 實作第一層節流：`LlmThrottlingBuffer`，將 LLM 碎塊合併（50~100ms 或 100 字元）。
- 實作第二層節流與併發防護：`SseSessionManager`，每個 User 獨立一個 `BlockingQueue` 並由單一 Executor 線程消費。
- 實踐明確且唯一的 `STREAM_END` 生命週期控制。
- 支援 SSE 的 `Last-Event-ID` 重連機制與伺服器 Mapping。

## Capabilities

### New Capabilities
- `stream-throttling`: 基於時間與累積字元的雙層節流機制，保護 Redis 與前端 TCP 連線不過載。
- `sse-session-management`: 單線程 User Queue 的 SSE 送信生命週期管理，包含 `Last-Event-ID` 斷線重連控制與結尾防呆。
- `llm-streaming-core`: 基於兩條核心 Redis Stream 運作的 Request/Response 廣播消費模型與路由架構。

### Modified Capabilities
- `stream-reliability`: 新的 `BotConsumer` (將拆分為 RequestConsumer / ResponseConsumer) 仍然需要遵循原有的 `PendingTaskScanner` 逾時回收掃描機制，但在架構上將改為從統一的 `stream:request` 進行任務回收。

## Impact

- `ChatBotController`: SSE 端點邏輯與請求發布邏輯將大幅度改寫，直接對接 `SseSessionManager`。
- `BotConsumer`: 將被拆分重構成負責呼叫 LLM 的 `RequestConsumer` 以及負責廣播給 SSE 的 `ResponseConsumer`。
- `SseEmitterManager`: 將被全新的 `SseSessionManager` 取代。
- Redis 配置：不再建立大量單戶 Stream，將統一配發 `MAXLEN ~1000` 於全局 Stream。
