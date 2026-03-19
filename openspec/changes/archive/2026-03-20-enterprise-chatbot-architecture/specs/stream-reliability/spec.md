## MODIFIED Requirements

### Requirement: Shared Throttle Timer Lifecycle
`LlmThrottlingBuffer` 的定時 flush 機制 SHALL 使用 Spring 管理的共享 `ScheduledExecutorService`，而非每個實例建立獨立的排程器。並且在串流由使用者主動中斷時確保資源的安全釋放。

#### Scenario: Normal flush lifecycle
- **WHEN** `LlmThrottlingBuffer` 被建立時
- **THEN** 它 SHALL 在共享排程器上註冊一個週期性任務，並持有 `ScheduledFuture` 參考

#### Scenario: Cleanup on stream completion
- **WHEN** `flushAndEnd()` 被呼叫時
- **THEN** 系統 SHALL 透過 `ScheduledFuture.cancel()` 取消該實例的定時任務

#### Scenario: Cleanup on error
- **WHEN** LLM 串流發生異常且 `flushAndEnd()` 未被呼叫時
- **THEN** `RequestConsumer` 的 `onErrorResume` 路徑 SHALL 呼叫 `LlmThrottlingBuffer.close()` 以取消定時任務並 flush 剩餘文字

#### Scenario: Cleanup on explicit user CANCEL/PAUSE
- **WHEN** 系統收到來自 Redis 控制鍵的 `PAUSE` 或 `CANCEL` 信號導致串流強制結束時
- **THEN** 系統 SHALL 中斷 LLM WebClient 連線並呼叫 `LlmThrottlingBuffer.close()` 妥善釋放計時器。
