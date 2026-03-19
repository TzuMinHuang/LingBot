## ADDED Requirements

### Requirement: Dual-Stream Unified Model
系統必須拋棄 Pub/Sub 架構，唯一採用 `stream:request` 與 `stream:response` 這兩條 Redis Stream 來進行任務派發與廣播結果。

#### Scenario: User sends a new prompt
- **WHEN** 前端透過 HTTP Request 發送訊息
- **THEN** 系統 SHALL 將提問打入 `stream:request`，等待 Worker 處理。

### Requirement: Multi-Node Broadcasting
當系統拓展至集群時，所有 Backend Instances 必須都能安全地收取 `stream:response` 但只負責向自己持有連線的使用者推送結果。

#### Scenario: Server fetches a response chunk
- **WHEN** 伺服器從 `stream:response` 的專屬 Group 讀取了一筆資料
- **THEN** 系統 SHALL 判斷該 `sessionId` 是否存活於本機，若是則進入 User Queue 消費，若否則略過，最後進行 XACK。
