## ADDED Requirements

### Requirement: 雙模 Token 廣播 (Mode A)
LLM Worker 在產生每一個 Token 時，必須同步透過 Redis Pub/Sub 頻道 `pubsub:chat:{sessionId}` 進行廣播。

#### Scenario: 即時顯示 Token
- **WHEN** Worker 接收到 LLM 產出的 Token
- **THEN** 前端應能透過 SSE 即時接收並顯示該 Token

### Requirement: 會話串流備份與補發 (Mode B)
LLM Worker 必須將所有 Token 依序寫入 Redis Stream `stream:chat:res:{sessionId}`，並設置 `MAXLEN`。

#### Scenario: 斷線重連數據恢復
- **WHEN** 前端 SSE 連線中斷後重新連線，並帶有 `Last-Event-ID`
- **THEN** API Server 從 Session Stream 中讀取自 ID 之後的所有數據並補發給前端
