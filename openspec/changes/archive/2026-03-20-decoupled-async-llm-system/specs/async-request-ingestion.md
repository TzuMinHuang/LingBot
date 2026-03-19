## ADDED Requirements

### Requirement: 使用 Redis Streams 進行請求排隊
API Server 必須將所有傳入的聊天請求轉化為 JSON 格式，並透過 `XADD` 指令寫入 `stream:bot:req`。

#### Scenario: 成功寫入請求隊列
- **WHEN** 使用者發送有效的聊天訊息
- **THEN** API Server 回傳訊息 ID 並將任務存入 Redis Stream，狀態設為「排隊中」

### Requirement: 消費者分組處理
多個 LLM Worker 實例必須加入 `bot-consumers` 群組，並使用 `XREADGROUP` 競爭獲取任務。

#### Scenario: 任務自動分配
- **WHEN** 隊列中有 5 個任務且有 2 個 Worker 在線
- **THEN** 任務應均勻分配給 Worker，且同一個任務不會同時被兩個 Worker 處理
