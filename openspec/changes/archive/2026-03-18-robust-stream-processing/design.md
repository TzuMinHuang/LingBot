## Context

在非同步 LLM 系統中，訊息的可靠性依賴於 Redis Stream 的 Consumer Group 機制。目前的 `PendingTaskScanner` 負責找出分配後超過 1 分鐘仍未 ACK 的訊息並進行 `XCLAIM`（收回）。

然而，原先的程式碼誤將 `Mono<PendingMessages>` 當作 `Flux<PendingMessage>` 處理，導致轉型失敗。

## Goals / Non-Goals

**Goals:**
- 修復 `PendingTaskScanner` 的功能，確保訊息復原機制運作正常。
- 確保 Reactive Event Loop 不會因為掃描解析錯誤而崩潰。
- 維護 Actuator 健康檢查的可用性。

**Non-Goals:**
- 修改 `BotConsumer` 的核心處理逻辑（LLM 呼叫部分）。
- 更改整體的 Redis Stream 拓撲結構。

## Decisions

### 1. Correct Unpacking of `PendingMessages`
- **問題**: `opsForStream().pending(...)` 返回的是 `Mono<PendingMessages>`，其中 `PendingMessages` 是一個包含 `List<PendingMessage>` 的封裝對象。
- **決策**: 使用 `.flatMapMany(Flux::fromIterable)` 來解開封裝，將單個物件轉換為訊息流。
- **理由**: 這是 Spring Data Redis Reactive 的標準處理方式，能避免 `ClassCastException`。

### 2. Idempotent Claiming
- **決策**: 持續使用 `XCLAIM` 與 `recovery-consumer`。
- **理由**: `XCLAIM` 本身是等冪的。如果多個實例同時試圖收回同一條訊息，Redis 會保證只有一個成功（或不產生副作用）。

### 3. Detailed Logging for Recovery
- **決策**: 在 `subscribe` 中加入具體的 Class Name 於 Error Log。
- **理由**: 有助於在生產環境快速區分是網路問題、Redis 錯誤還是業務邏輯轉型錯誤。

## Risks / Trade-offs

- **[Risk]** 訊息重複領取 → **[Mitigation]** `BotConsumer` 已具備一定的 idempotency 處理，且 `XCLAIM` 會更新訊息的 Idle Time，防止短期內被重複收回。
- **[Risk]** 掃描頻率過高導致 Redis 負擔 → **[Mitigation]** 目前設定為 1 分鐘一次，且每次限制抓取 100 筆，對系統影響極小。
