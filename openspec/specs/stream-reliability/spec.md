## ADDED Requirements

### Requirement: Automatic Pending Message Recovery
系統必須定期掃描 Redis Stream 中處於 Pending 狀態且超過 60 秒未確認的訊息，將其重新分配給復原消費者，並實際重新處理該訊息。超過 3 次投遞失敗的訊息 SHALL 被移至 DLQ。

#### Scenario: Re-claiming and reprocessing timed-out messages
- **WHEN** 一則訊息被分配給消費者後超過 60 秒未被 ACK
- **THEN** `PendingTaskScanner` SHALL 使用 `XCLAIM` 將其所有權變更為 `recovery-consumer`，並呼叫 `RequestConsumer.processRecord()` 重新處理該訊息

#### Scenario: Message moved to DLQ after max retries
- **WHEN** 一則訊息的投遞次數超過 3 次（從 PEL metadata 的 `deliveryCount` 判定）
- **THEN** 系統 SHALL 將該訊息移至 `stream:bot:dlq` 並 ACK 原始訊息，不再重試

### Requirement: Robust Error Isolation in Background Tasks
背景掃描與復原任務必須具備獨立的異常隔離機制。任何資料處理或轉型錯誤不得導致 Spring 計時器線程阻塞，亦不得影響 Actuator 健康檢查的響應。

#### Scenario: Graceful failure handling
- **WHEN** 掃描過程中發生 Reactive 操作異常（如 `ClassCastException` 或 Redis 連線問題）
- **THEN** 系統 SHALL 攔截異常、發送錯誤日誌，並確保下一個掃描週期能正常啟動，同時 HTTP 健康檢查（`/actuator/health`）應能在 5 秒內正常響應。

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

### Requirement: Per-Node Response Consumer Group
每個 Backend 節點 SHALL 在 `stream:response` 上建立獨立的 consumer group（以 instanceId 區分），確保每個節點都能收到所有回應訊息。

#### Scenario: Multi-node message delivery
- **WHEN** `RequestConsumer` 在節點 A 寫入一則回應至 `stream:response`
- **THEN** 節點 A 和節點 B 的 `ResponseConsumer` 都 SHALL 收到該訊息

#### Scenario: Local session filtering
- **WHEN** `ResponseConsumer` 收到一則回應訊息但該 sessionId 不在本機
- **THEN** 系統 SHALL ACK 該訊息但不推送至 `SseSessionManager`

#### Scenario: Stale consumer group cleanup
- **WHEN** 一個 Backend 節點啟動時
- **THEN** `RedisStreamInitializer` SHALL 檢查 `stream:response` 上的所有 consumer groups，並移除心跳 key 已過期（TTL 5 分鐘）的陳舊 groups
