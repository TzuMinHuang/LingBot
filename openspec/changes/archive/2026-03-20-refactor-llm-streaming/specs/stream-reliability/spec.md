## MODIFIED Requirements

### Requirement: Automatic Pending Message Recovery
系統必須定期掃描統一的 `stream:request` 中處於 Pending 狀態且超過 60 秒未確認的訊息，並將其重新分配給復原消費者處理。

#### Scenario: Re-claiming timed-out messages
- **WHEN** 一則提問任務被分配給 `RequestConsumer` 後超過 60 秒未被 ACK
- **THEN** `PendingTaskScanner` 應偵測到該訊息並使用 `XCLAIM` 將其所有權變更為 `recovery-consumer`，以利重試流程
