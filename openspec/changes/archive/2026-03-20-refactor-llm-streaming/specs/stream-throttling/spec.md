## ADDED Requirements

### Requirement: Double Layer Throttling
系統必須實施雙層 (Redis 端與 SSE 端) Throttling 機制，利用時間與字元長度雙參數進行緩衝合併，以確保在高低負載下皆能避免過度頻繁的 I/O 次數。

#### Scenario: LLM returns text chunk
- **WHEN** LLM 端每毫秒返回微小字串
- **THEN** 系統 SHALL 將該字串暫存於 `LlmThrottlingBuffer`，直到超過 100 個字元或距離上次 Flush 超過 50ms 時，才發送至 `stream:response`。

#### Scenario: LLM streaming completes
- **WHEN** LLM 結束串流回覆
- **THEN** 系統 SHALL 把 buffer 中剩餘的字全數 Flush 寫入 Stream，並隨即追加一則 `type: "END"` 的訊息。
