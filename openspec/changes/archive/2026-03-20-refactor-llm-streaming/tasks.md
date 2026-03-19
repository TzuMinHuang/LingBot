## 1. Infrastructure Configuration

- [x] 1.1 設定 `RedisStreamConfig` 以註冊 `stream:request` 與 `stream:response` 的雙 Stream 架構，並設定 MAXLEN 約 1000。
- [x] 1.2 建立 `ThreadPoolConfig` 提供 50~100 大小的 `ExecutorService` 供 SSE 單線程發送使用。

## 2. Shared Utilities Implementation

- [x] 2.1 實作 `LlmThrottlingBuffer` 雙層節流機制，提供 `append` (滿 100 字元或 50ms 寫入)、`flushAndEnd` 等方法。
- [x] 2.2 實作 `SseSessionManager` 持有 Session 與單一 `BlockingQueue` 之映射，並負責單線程提領與發送。
- [x] 2.3 於 `SseSessionManager` 中加入 `Last-Event-ID` 讀取能力與斷線重連機制。

## 3. Consumer Refactoring

- [x] 3.1 拆分並實作 `RequestConsumer`，專職監聽 `stream:request`，呼叫 LLM 服務並將結果餵入 `LlmThrottlingBuffer`。
- [x] 3.2 拆分並實作 `ResponseConsumer`，監聽 `stream:response`（廣播模式），檢查 `isLocalSession` 後推送至 `SseSessionManager`。
- [x] 3.3 修改 `PendingTaskScanner` 的掃描目標，從原機制轉為針對 `stream:request` 處理逾時 60 秒未 ACK 的重試任務 (`recovery-consumer`)。

## 4. API Endpoints Update

- [x] 4.1 更新 `ChatBotController` 的 `/chat/{sessionId}/send` 與 `/chat/{sessionId}/stream` 端點，移除 Pub/Sub，對接 `stream:request`。
- [x] 4.2 清除舊有的 `SseEmitterManager` 與 `SseRedisSubscriber` (Pub/Sub) 相關類別與設定檔。
- [x] 4.3 進行單機壓測與連線中斷重連測試，驗證 1000 users 併發與資料不遺失/不亂序。
