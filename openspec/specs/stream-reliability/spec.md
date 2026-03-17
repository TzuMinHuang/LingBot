## ADDED Requirements

### Requirement: Automatic Pending Message Recovery
系統必須定期掃描 Redis Stream 中處於 Pending 狀態且超過 60 秒未確認的訊息，並將其重新分配給復原消費者處理。

#### Scenario: Re-claiming timed-out messages
- **WHEN** 一則訊息被分配給消費者後超過 60 秒未被 ACK
- **THEN** `PendingTaskScanner` 應偵測到該訊息並使用 `XCLAIM` 將其所有權變更為 `recovery-consumer`

### Requirement: Robust Error Isolation in Background Tasks
背景掃描與復原任務必須具備獨立的異常隔離機制。任何資料處理或轉型錯誤不得導致 Spring 計時器線程阻塞，亦不得影響 Actuator 健康檢查的響應。

#### Scenario: Graceful failure handling
- **WHEN** 掃描過程中發生 Reactive 操作異常（如 `ClassCastException` 或 Redis 連線問題）
- **THEN** 系統 SHALL 攔截異常、發送錯誤日誌，並確保下一個掃描週期能正常啟動，同時 HTTP 健康檢查（`/actuator/health`）應能在 5 秒內正常響應。
