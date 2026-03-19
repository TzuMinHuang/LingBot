## ADDED Requirements

### Requirement: Stream Lag Metrics
系統 SHALL 透過 Micrometer 暴露 `stream.request.lag` 和 `stream.response.lag` gauge 指標，反映各 stream 的消費延遲（pending message count）。

#### Scenario: Lag metric reflects pending count
- **WHEN** `stream:request` 有 15 則 pending messages
- **THEN** `stream.request.lag` gauge SHALL 回報值為 15

#### Scenario: Lag metric available via Actuator
- **WHEN** 客戶端呼叫 `/actuator/metrics/stream.request.lag`
- **THEN** 回應 SHALL 包含當前 lag 值且 HTTP status 為 200

### Requirement: PEL Size Metrics
系統 SHALL 暴露 `stream.request.pel.size` gauge，反映 request consumer group 的 Pending Entries List 大小。

#### Scenario: PEL size tracks unacknowledged messages
- **WHEN** 有 3 則訊息被消費但尚未 ACK
- **THEN** `stream.request.pel.size` gauge SHALL 回報值為 3

### Requirement: Active SSE Session Count
系統 SHALL 暴露 `sse.active.sessions` gauge，反映當前本機活躍的 SSE 連線數。

#### Scenario: Session count reflects connected clients
- **WHEN** 有 42 個前端 SSE 連線處於活躍狀態
- **THEN** `sse.active.sessions` gauge SHALL 回報值為 42

### Requirement: Consumer Idle Time
系統 SHALL 暴露 `consumer.idle.seconds` gauge（以 consumer name 為 tag），反映各消費者最後一次處理訊息至今的秒數。

#### Scenario: Idle time increases when no messages
- **WHEN** 消費者 `req-consumer-abc-1` 已 90 秒未處理任何訊息
- **THEN** `consumer.idle.seconds{consumer="req-consumer-abc-1"}` SHALL 回報值 ≥ 90
