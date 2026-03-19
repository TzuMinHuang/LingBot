## ADDED Requirements

### Requirement: Single-Threaded User Queue
每個連線的使用者必須具備一條唯一的 `BlockingQueue` 來緩存待發送的 Chunk，並且只能被一個獨立執行的 Consumer 線程所消費，絕不允許多線程並發送出 SSE 以確保絕對順序。

#### Scenario: Server processes response chunks
- **WHEN** `ResponseConsumer` 從 Redis Stream 收取屬於本節點的 Chunk 後
- **THEN** 系統 SHALL 將這些 Chunk 循序推送至該使用者的單一 BlockingQueue 中，並由特定線程以 50ms 為週期輪詢並封裝後透過 `tryEmitNext`/`send` 給前端 SSE。

### Requirement: Last-Event-ID State Recovery
SSE 端點必須支援讀取前端帶入的 `Last-Event-ID` 標頭，並以此建立從該點恢復發佈未讀訊息的能力。

#### Scenario: User reconnects seamlessly
- **WHEN** 使用者網路瞬斷帶有 `Last-Event-ID` 重連時
- **THEN** 系統 SHALL 從 `stream:response` 中的該筆 ID 提供 offset 檢索未發送的 Chunk，並交由 Queue 流水線接續傳送。
