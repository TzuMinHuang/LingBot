## Context

目前系統在處理 LLM Streaming 時，依賴 Pub/Sub 機制以及為每個使用者動態產生專屬的 Redis Stream，缺乏有效的降頻合併（Throttling）手段，並且並行的 WebFlux 推送未加諸嚴謹的次序控管。當系統面臨 1000 位使用者同時在線的負載時，大量細碎的 LLM Chunk 導致 Redis QPS 爆發與伺服器網路 I/O 擁塞，增加亂序與資源耗盡的風險。

## Goals / Non-Goals

**Goals:**
* 減少系統中的 Redis Stream 數量至唯一的 `request` 與 `response` 兩條。
* 導入雙層 Throttling (節流) 機制，大幅降低 Redis 讀寫頻率與 SSE 發送頻率。
* 使用單一線程的 BlockingQueue 來管理每個 Session 的發送，確保 100% 不亂序。
* 建立基礎架構以支援未來擴充多台 Backend Server（如 Last-Event-ID 重連與 Mapping）。

**Non-Goals:**
* 實作分散式 Redis Cluster 或更換基礎資料庫架構（目前維持單點或標準高可用 Redis 即可）。
* 修改 LLM 呼叫核心邏輯（維持呼叫 AnythingLLM API）。

## Decisions

1. **僅使用兩條 Redis Stream**
   * *Rationale*: 拋棄 Pub/Sub 與動態 Stream。集中使用 `stream:request` 與 `stream:response` 兩條主幹道搭配 Consumer Group。這讓維運監控更容易，並自然適應多機廣播模型。
   * *Alternatives*: 維持各自獨立 Stream，但會遭遇 Redis 金鑰空間膨脹與難以管理。
2. **導入 LlmThrottlingBuffer 雙條件節流**
   * *Rationale*: 規定 `50~100ms` 或 `100 chars` 才能寫入 Redis，將 1000 users 同時下發的 Chunk I/O 操作大幅減少 90% 以上。
   * *Alternatives*: 收一個字發一個字，會導致網路與 Redis 癱瘓。
3. **SseSessionManager 採用單一 Queue 單線程模型**
   * *Rationale*: 為每個使用者配備 `BlockingQueue` 並交給 `FixedThreadPool` 單獨消費。此法完全杜絕 `@Async` 引發的併發碰撞與 `STREAM_END` 多發問題。

## Risks / Trade-offs

* **[Risk] Throttling 導致體感延遲增加**
  → *Mitigation*: 50ms 仍在人類視覺暫留極限內，加上平滑的打字機效果，使用者體驗反倒會因穩定而更好。
* **[Risk] 1000 Users 持有 1000 條執行緒耗盡資源**
  → *Mitigation*: 共用 50~100 大小的 Thread Pool 來進行 Queue 消費。因為有 Timeout 設定，不需要 1:1 綁定執行緒。
* **[Risk] 斷線重連導致漏接**
  → *Mitigation*: 要求前端傳遞 `Last-Event-ID`，讓 Backend 可以從 `stream:response` 的偏移量（Offset）恢復發送尚未輸出的 Chunk。
