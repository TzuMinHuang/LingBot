## ADDED Requirements

### Requirement: Bounded Throttle Buffer
`LlmThrottlingBuffer` SHALL 使用有界佇列（預設容量 512 個 chunk）來累積 LLM 碎塊，防止單一請求的記憶體無限成長。

#### Scenario: Normal operation within capacity
- **WHEN** LLM 串流產生的未 flush 碎塊數量低於佇列容量
- **THEN** 所有碎塊 SHALL 被正常累積並按照既有的字元閾值（100 字元）或時間閾值（50ms）flush

#### Scenario: Buffer overflow with drop-oldest strategy
- **WHEN** 未 flush 的碎塊數量達到佇列容量上限
- **THEN** 系統 SHALL 丟棄最舊的未 flush 碎塊，記錄一則 WARN 級別日誌，並繼續接收新碎塊

### Requirement: Configurable Buffer Capacity
緩衝區容量 SHALL 可透過 Spring 配置屬性 `app.throttle-buffer-capacity` 進行調整。

#### Scenario: Custom capacity configuration
- **WHEN** `application.yml` 中設定 `app.throttle-buffer-capacity: 256`
- **THEN** `LlmThrottlingBuffer` 的佇列容量 SHALL 為 256
