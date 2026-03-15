# LingBot 架構說明

## 目錄

- [系統概觀](#系統概觀)
- [訊息流程](#訊息流程)
- [後端模組](#後端模組)
  - [進入點](#進入點)
  - [設定層 config](#設定層-config)
  - [控制器層 controller](#控制器層-controller)
  - [消費者層 consumer)](#消費者層-consumer)
  - [服務層 service](#服務層-service)
  - [客戶端 client](#客戶端-client)
  - [動作層 action](#動作層-action)
  - [資料層 entity--repo](#資料層-entity--repo)
  - [資料傳輸物件 dto](#資料傳輸物件-dto)
  - [通用模組 common](#通用模組-common)
- [前端模組](#前端模組)
- [基礎設施](#基礎設施)
- [環境設定](#環境設定)

---

## 系統概觀

LingBot 是一個 AI 聊天機器人平台，結合 Spring Boot 後端、Redis 訊息佇列、PostgreSQL 持久化，以及 AnythingLLM 作為 LLM 推理引擎。

```
瀏覽器 (SockJS / STOMP)
   ↕
Nginx (port 80)
   ↕
backend-service (Spring Boot, port 9200)
   ↕                    ↕
Redis Streams       PostgreSQL
   ↕
AnythingLLM (port 3001)  ← SSE Streaming
```

---

## 訊息流程

### 使用者傳送訊息

```
[前端] 使用者輸入
  → ChatApp._handleUserMessage()
  → ChatService.sendMessage()
  → WebSocketClient.sendMessage()   STOMP: /app/chat/{sessionId}/send
  → ChatWebSocketController.incomingUser()
  → Redis Stream: stream:bot:incoming  [sessionId, content]
  → BotConsumer.onMessage()
  → AnythingLLMClient.chatStream()  POST /api/v1/workspace/{slug}/stream-chat
  → SSE 回應 (每個 token 一個 chunk)
  ┌─ chunk (close=false): EventDto{type=STREAM_CHUNK, payload.content=token}
  └─ close (close=true):  EventDto{type=STREAM_END,   payload.sources=[...]}
  → Redis Pub/Sub: ws-channel
  → WebSocketConfig.webSocketSubscriber()
  → SimpMessagingTemplate → STOMP: /topic/user/{sessionId}/receive
  → ChatApp message handler
  ┌─ STREAM_CHUNK → ChatUI.appendToStreamBubble(chunk)  逐字累加
  └─ STREAM_END   → ChatUI.finishStreamBubble(sources)  附加參考資料
```

### 工作階段初始化

```
[前端] 頁面載入
  → ChatApp._initSession()
  → POST /bot/chat/initial
  → ChatSessionController.initialClientInfo()
  → ChatSessionService.initialClientInfo()
     生成 sessionId (Hashids), userId
     Redis 寫入 chat:session:{sessionId}
  → 回傳 InitialClientInfo{sessionId}
  → ChatService 建立 WebSocket 連線
  → STOMP SUBSCRIBE /topic/user/{sessionId}/receive
  → WebSocketConfig 攔截 → WebSocketSessionManager.addSession()
```

---

## 後端模組

### 進入點

| 類別 | 套件 | 說明 |
|------|------|------|
| `BotApplication` | `idv.hzm.app.bot` | Spring Boot 主程式，啟用 `@EnableScheduling` |

---

### 設定層 config

| 類別 | 說明 | 關鍵 Bean |
|------|------|-----------|
| `WebSocketConfig` | STOMP 端點設定、Redis Pub/Sub → WebSocket 橋接 | `RedisMessageListenerContainer`、`MessageListenerAdapter` |
| `WebSocketSessionManager` | 追蹤 businessSessionId ↔ stompSessionId 對應關係 | — |
| `RedisStreamConfig` | Redis Stream 監聽容器，poll 間隔 200ms | `StreamMessageListenerContainer` |
| `RedisConfig` | Redis template，value 序列化為 JSON | `RedisTemplate<String, Object>` |
| `RestTemplateConfig` | 同步 HTTP 客戶端 | `RestTemplate` |
| `WebConfig` | CORS 設定 | — |
| `ChatConfig` | MCP Server 工具客製化（logging） | `McpSyncClientCustomizer` |
| `RabbitMQConfig` | 已停用（RabbitMQ 已移除） | — |

#### WebSocketConfig 詳細

- STOMP 端點：`/ws`（含 SockJS fallback）
- Broker prefix：`/topic`；Application prefix：`/app`
- 攔截 `SUBSCRIBE /topic/user/{sessionId}/receive`，登錄至 `WebSocketSessionManager`
- Redis channel `ws-channel` 訊息 → `EventDto` JSON → `SimpMessagingTemplate.convertAndSend()`

#### WebSocketSessionManager 詳細

```
userSessions:          Map<businessSessionId, Set<stompSessionId>>
stompToBusinessSession: Map<stompSessionId, businessSessionId>
lastHeartbeat:         Map<stompSessionId, Long>

addSession(businessSessionId, stompSessionId)
removeSession(stompSessionId)
updateHeartbeat(stompSessionId)
cleanInactiveSessions()  ← @Scheduled 每 30 秒執行，逾時閾值 2 分鐘
@EventListener SessionDisconnectEvent → 自動清除
```

#### RedisStreamConfig 常數

```
BOT_INCOMING_STREAM  = "stream:bot:incoming"
BOT_EXECUTE_STREAM   = "stream:bot:execute_request"
BOT_RESPOND_STREAM   = "stream:bot:execute_respond"
CONSUMER_GROUP       = "bot-consumer-group"
```

---

### 控制器層 controller

| 類別 | 端點 | 說明 |
|------|------|------|
| `ChatSessionController` | `POST /chat/initial` | 建立工作階段，回傳 `InitialClientInfo{sessionId}` |
| `ChatWebSocketController` | `@MessageMapping /chat/{sessionId}/ping` | WebSocket 心跳（no-op） |
| `ChatWebSocketController` | `@MessageMapping /chat/{sessionId}/send` | 接收使用者訊息，寫入 `stream:bot:incoming` |

---

### 消費者層 consumer

#### `BotConsumer`

```
implements StreamListener on: stream:bot:incoming
consumer group: bot-consumer-group
consumer name:  bot-consumer-1

onMessage(record):
  sessionId = record["sessionId"]
  content   = record["content"]
  anythingLLMClient.chatStream(content, sessionId)
    .doOnNext(event):
      if close == true:
        sources = event["sources"]
        publish STREAM_END EventDto → ws-channel
      else:
        chunk = event["textResponse"]
        publish STREAM_CHUNK EventDto → ws-channel
    .blockLast()
```

#### `ExcuetConsumer`

```
implements StreamListener on: stream:bot:execute_request
consumer group: bot-consumer-group
consumer name:  execute-consumer-1

onMessage(record):
  content = record["content"]
  result  = actionService.handle(content)
  寫入 stream:bot:execute_respond
```

#### `ReplyUserConsumer`

已停用（原為 RabbitMQ 監聽器，現在由 BotConsumer 直接發布至 ws-channel）。

---

### 服務層 service

| 類別 | 依賴 | 關鍵方法 |
|------|------|---------|
| `ChatSessionService` | `CodeGeneratorUtil`, `SessionsRepository`, `ChatRedisService` | `initialClientInfo()` → 生成 Session，存 Redis |
| `ChatRedisService` | `StringRedisTemplate`, `RedisTemplate<String,Object>`, `ObjectMapper` | 管理 Session / User / Room / Queue / Agent 的 Redis 資料 |
| `UserService` | `ChatRedisService`, `CodeGeneratorUtil` | `createCustomerUser()` → `UserDto` |
| `ActionService` | `List<Action>` | `handle(content)` → 依 type 分派至對應 Action |
| `OllamaService` | — | stub，未實作 |
| `RabbitMQService` | — | stub，RabbitMQ 已移除 |

#### `ChatRedisService` Redis Key 結構

```
chat:session:{sessionId}   → Session JSON (TTL 5 min)
user:{userId}              → UserDto JSON
chat:room:{sessionId}      → agentId
queue:waiting              → Redis List (FIFO 等待隊列)
online:agents              → Redis Set
bind:user:{userId}         → agentId
```

---

### 客戶端 client

#### `AnythingLLMClient`

```
baseUrl        : ${anythingllm.base-url}
apiKey         : ${anythingllm.api-key}
workspaceSlug  : ${anythingllm.workspace-slug}

chatStream(message, sessionId) → Flux<Map<String, Object>>
  POST {baseUrl}/api/v1/workspace/{slug}/stream-chat
  Accept: text/event-stream
  Authorization: Bearer {apiKey}
  Body: { message, mode: "chat", sessionId }
  Timeout: 3 分鐘
  回傳每個 SSE event 解析後的 Map（error 事件過濾，close 事件穿透）
```

SSE Event 結構（AnythingLLM 回傳）：
```json
{ "textResponse": "token", "close": false, "error": false, "sources": [] }
{ "textResponse": "",      "close": true,  "error": false, "sources": [{"title":"...","url":"..."}] }
```

#### `QueueClient`

```
baseUrl: ${admin.base-url}

enqueue(sessionId), cancelQueue(), dequeue()
getStatus(), getQueueInfo()
→ 呼叫 admin-service REST API
```

---

### 動作層 action

```
Action<T> (abstract)
├── FqaAction   getType()="FQA",   handle(FqaDto)
└── LeaveAction getType()="Leave", handle(LeaveDto)

ActionService 在啟動時自動掃描 Action bean，建立 Map<type, Action>
```

---

### 資料層 entity / repo

#### JPA 實體

| 實體 | 資料表 | 說明 |
|------|--------|------|
| `Session` | sessions | 聊天工作階段（sessionId, userId, status, startTime） |
| `Event` | events | 聊天事件紀錄（sessionId, type, senderType, payload JSONB） |
| `Topic` | topic | 知識主題 |
| `Subtopic` | subtopic | 主題子類別，關聯 Topic |
| `Intent` | intent | 意圖定義，關聯 Subtopic |
| `Issue` | issues | 使用者問題工單，關聯 Topic |
| `RobIntent` | rob_intent | Rasa Bot 意圖定義 |
| `RobExample` | rob_examples | Rasa 訓練範例，關聯 RobIntent |
| `ProcessInstance` | process_instance | 流程實例（UUID PK） |
| `ProcessStep` | process_step | 流程步驟，關聯 ProcessInstance |
| `ProcessEventLog` | process_event_log | 流程事件日誌 |

#### Repository

所有 Repository 繼承 `JpaRepository` 或 `CrudRepository`，對應上列實體。

---

### 資料傳輸物件 dto

#### `EventDto` — 主要 WebSocket 傳輸單元

```java
String sessionId
String type      // "MESSAGE" | "STREAM_CHUNK" | "STREAM_END"
BasePayload payload
```

#### `BasePayload` — Jackson 多型基底

```java
@JsonTypeInfo(property = "type")
@JsonSubTypes({ "message" → MessagePayload })
```

#### `MessagePayload` extends BasePayload

```java
String content                       // token 文字 or 完整訊息
List<Map<String, Object>> sources    // 參考資料（僅 STREAM_END 使用）
```

#### 其他 DTO

| 類別 | 欄位 |
|------|------|
| `InitialClientInfo` | sessionId |
| `UserDto` | userId, role (UserRole enum) |
| `QueueInfoDto` | sessionId, isAlreadyInQueue, position |
| `FqaDto` | content |
| `LeaveDto` | id, name, startTime, endTime |
| `CommonActionDto` | type, data |

#### Enum

| 名稱 | 值 |
|------|-----|
| `UserRole` | CUSTOMER, AGENT, SUPERVISOR, MAINTAINER |
| `PayloadType` | MESSAGE |

---

## 通用模組 common

位於 `idv.hzm.app.common`，與 bot 同一 Maven 模組。

### API 回應框架

```
CommonResult<T>    { code, message, data }
ResultCode (enum)  SUCCESS(200) / FAILED(500) / VALIDATE_FAILED(404) / UNAUTHORIZED(401) / FORBIDDEN(403)
IErrorCode         interface: getCode(), getMessage()
```

### 工具類別

| 類別 | 說明 |
|------|------|
| `CodeGeneratorUtil` | Hashids 編碼的 ID 生成器，使用 Redis counter（room / user / session） |
| `Json2Util` | ObjectMapper 的 JSON 序列化/反序列化包裝 |
| `JSONUtil` | Hutool JSON 包裝 |
| `StrUtil` / `DateUtil` / `CollUtil` / `URLUtil` | 工具類包裝 |

### Redis ID Counter Key

```
global:room:counter
global:user:counter
global:chat:session
```

---

## 前端模組

### 模組依賴圖

```
App.js
  └── ChatApp.js
        ├── ChatUI.js
        ├── ChatService.js
        │     └── Websocket.js (WebSocketClient)
        └── EventBus.js
```

### 各模組說明

#### `App.js`
進入點，DOMContentLoaded 後建立 `ChatApp("/bot")`。

#### `ChatApp`
主協調器。管理 session 初始化、UI 事件綁定、WebSocket 訊息路由。

```javascript
_initSession()       POST /bot/chat/initial → sessionId → 建立 ChatService
_handleUserMessage() 顯示使用者訊息 → showThinking() → sendMessage()
onMessage 路由:
  STREAM_CHUNK → chatUI.appendToStreamBubble(content)
  STREAM_END   → chatUI.finishStreamBubble(sources)
  其他         → eventBus.emit('incoming', content)
```

#### `ChatUI`
純 DOM 操作，無狀態。

```javascript
showOutgoing(message)          顯示使用者訊息 bubble
showIncoming(message)          顯示機器人訊息 bubble（取代 Thinking...）
showThinking()                 顯示 "Thinking..." placeholder
appendToStreamBubble(chunk)    串流：第一個 chunk 取代 Thinking...，後續累加
finishStreamBubble(sources)    串流結束：移除 data-streaming，附加參考資料
_appendSources(li, sources)    渲染 .sources-container 參考資料標籤
```

#### `ChatService`
薄包裝層，持有 `WebSocketClient` 實例，代理 `sendMessage()`。

#### `WebSocketClient`（Websocket.js）
```
連線: SockJS → Stomp.over()
訂閱: /topic/user/{sessionId}/receive
發送: /app/chat/{sessionId}/send
心跳: 每 30 秒 ping /app/chat/{sessionId}/ping
重連: 指數退避，1s → 最大 30s
閒置逾時: 4 分鐘無操作 → 1 分鐘後斷線
```

#### `EventBus`
簡易 pub/sub，`on(event, handler)` / `emit(event, data)` / `off()`。

#### `chatbot-plugin.js`
嵌入式 chatbot 外掛，建立浮動按鈕與 iframe，iframe src = `/chatbot.html`。

---

## 基礎設施

### Docker Compose 服務

| 服務 | Image | Port | 說明 |
|------|-------|------|------|
| `redis` | redis:7 | 6379 | Redis Streams + Pub/Sub + Cache |
| `postgres` | pgvector/pgvector:pg16 | 5432 | 主資料庫（支援向量欄位） |
| `pgadmin` | dpage/pgadmin4 | 5050 | 資料庫管理 UI |
| `anythingllm` | mintplexlabs/anythingllm | 3001 | LLM 推理 + RAG |
| `backend-service` | 本地建置 | 9200 | Spring Boot 主服務 |
| `nginx` | nginx:alpine | 80 | 前端靜態檔案 + 反向代理 |

### Nginx 路由規則

```
/chatbot     → frontend/index.html (SPA)
/bot/        → backend-service:9200/bot/
/bot/ws/     → backend-service:9200/bot/ws/  (WebSocket upgrade)
/chat/       → backend-service:9200/chat/
```

---

## 環境設定

### Dev Profile（本機開發）

| 設定 | 值 |
|------|-----|
| Server port | 9200 |
| Context path | /bot |
| PostgreSQL | localhost:5432 / chatdb |
| Redis | localhost:16379 |
| AnythingLLM | http://localhost:3001 |

### Test Profile（Docker 容器）

| 環境變數 | 說明 |
|----------|------|
| `DATASOURCE_URL` | PostgreSQL host |
| `DATASOURCE_PORT` | PostgreSQL port |
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port |
| `ANYTHINGLLM_BASE_URL` | AnythingLLM URL |
| `ANYTHINGLLM_API_KEY` | API 金鑰 |
| `ANYTHINGLLM_WORKSPACE_SLUG` | Workspace ID |
| `ADMIN_IP` / `ADMIN_PORT` | admin-service 位址 |

---

## 技術選型摘要

| 技術 | 用途 |
|------|------|
| Spring Boot 3.4.5 | 後端框架 |
| Spring WebSocket (STOMP) | 即時雙向通訊 |
| Spring WebFlux (WebClient) | 非同步 SSE 消費 |
| Redis Streams | 非同步訊息佇列（取代 RabbitMQ） |
| Redis Pub/Sub | LLM 回應廣播至 WebSocket |
| PostgreSQL + pgvector | 持久化 + 向量欄位支援 |
| AnythingLLM | LLM 推理引擎 + RAG |
| Jackson | JSON 多型序列化 |
| Hashids | 使用者/工作階段 ID 編碼 |
| SockJS + STOMP.js | 前端 WebSocket 客戶端 |
| Vanilla JS (ES6 Modules) | 前端（無框架） |
