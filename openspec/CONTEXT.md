# Project Context: LingBot

## System Overview
LingBot 是一個整合大語言模型 (LLM) 的全端 AI 聊天應用程式，旨在提供工業級的問答服務。系統採用非同步串流架構，確保高併發環境下的穩定性與反應速度。

## Backend Architecture (Spring Boot 3)
後端基於 **Spring Boot 3.4.5** 與 **Java 17** 構建，核心邏輯圍繞 Redis 進行解耦與狀態管理：

- **核心技術**: Spring WebFlux (WebClient), Spring Data JPA, Spring Data Redis (Lettuce), Spring AI (BOM 管理)。
- **資料持久化 (Postgres)**: 
  - 使用 `pgvector` 支援向量搜尋。
  - 儲存對話歷史 (`ChatMessage`)、使用者 Session (`Session`) 等結構化數據。
- **緩存與非同步任務 (Redis)**:
  - **Redis Streams (`stream:bot:incoming`)**: 用於處理傳入的聊天訊息。`ChatBotController` 將訊息寫入 Stream，由多個 `BotConsumer` 實例競爭消費，實現負載均衡與削峰填谷。
  - **Redis Pub/Sub (`sse:chat:events`)**: 消費者處理完 LLM 請求後，透過 Pub/Sub 廣播事件。
  - **分布式鎖**: 透過 Redis 實現請求鎖 (`acquireRequestLock`)，確保單一 Session 同一時間僅處理一個提問，保證冪等性。
  - **SSE Emitter 管理**: `SseEmitterManager` 結合 `SseRedisSubscriber` 訂閱 Pub/Sub 頻道，並將即時訊息推送到前端。
- **健康監測**: 整合 Spring Boot Actuator 與 JavaMelody 提供即時監控。

## Frontend Implementation
前端是一個純 JavaScript/HTML/CSS 的 SPA 應用，透過 Nginx 代理與後端互動：

- **通訊協議**:
  - **REST API**: 用於初始化 Session (`/chat/initial`)、發送訊息 (`/chat/{sessionId}/send`)、獲取歷史紀錄 (`/chat/{sessionId}/history`) 及熱門建議 (`/chat/suggestions`)。
  - **SSE (Server-Sent Events)**: 透過 `EventSource` 訂閱串流響應 (`/chat/{sessionId}/stream`)，實現打字機效果。
- **狀態管理**: 透過 `EventBus.js` 進行組件間通訊，`ChatService.js` 封裝所有 API 邏輯。
- **UI/UX**: 具備工業風格設計 (Industrial UI)，支援自動重連、手動停止生成 (`/stop`) 及排隊位置查詢。

## AI/LLM Integration
系統採用多層級的 AI 基礎架構，兼顧本地隱私與雲端能力：

- **LiteLLM (API 網關)**:
  - 作為統一的模型調度層，管理本地模型 (Ollama/llama3.1) 與外部 API (OpenAI, Anthropic)。
  - **Redis 緩存**: LiteLLM 配置了 Redis (DB 1) 作為回應快取，減少重複請求的成本。
  - **路由策略**: 使用 `least-busy` 策略進行負載平衡。
- **AnythingLLM (知識庫管理/RAG)**:
  - 負責文件的向量化 (Embedding) 與知識庫檢索。
  - **整合方式**: 後端 `AnythingLLMClient` 透過其 REST API 進行 `/stream-chat` 互動。
  - **邏輯流**: 
    1. 前端發送訊息 -> 後端寫入 Redis Stream。
    2. `BotConsumer` 調用 `AnythingLLMClient`。
    3. AnythingLLM 透過 LiteLLM 獲取 LLM 響應與 Embedding。
    4. 後端解析 SSE Chunk (支援 sources 引用) 並透過 Redis Pub/Sub 推送回前端。
- **Ollama**: 在本地運行 Llama 3.1 8B 與 mxbai-embed-large 模型，提供純地端運行能力。

## Infrastructure Management
- **Docker Compose**: 透過 Profiles 分離基礎設施 (`infra`) 與應用層 (`app`)。
- **自動化設定**: 各組件具備 init 腳本 (如 `ensure-models.sh`, `ensure-settings.sh`, `ensure-db.sh`)，實現環境冪等初始化。
