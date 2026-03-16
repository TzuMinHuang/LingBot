# 🤖 LingBot — 智慧對話流程引擎

**LingBot** 是一個基於 Spring Boot 3.4.5 開發，結合 AnythingLLM 推理引擎與 Redis 串流技術的智慧對話系統。支援多實例負載平衡、請求冪等性防護，並透過 Server-Sent Events (SSE) 提供即時的對話與排隊狀態反饋。

---

## 🧰 開發框架與語言

- **後端**：Java 17+, Spring Boot 3.4.5
- **前端**：Vanilla JavaScript (ES6+), CSS3 (Glassmorphism), HTML5
- **推理引擎**：AnythingLLM (RAG 支援)

## 🗃 基礎設施

- **資料持久化**：PostgreSQL (對話紀錄與使用者資料)
- **快取與消息隊列**：Redis
  - **Redis Streams**：非同步處理 AI 回覆任務
  - **Redis Pub/Sub**：跨實例事件同步
  - **Redis Distributed Lock/Key**：實現提問冪等性
- **即時通訊**：Server-Sent Events (SSE)

---

## 🔧 核心功能

### 🚀 高可用與擴展性
- **多實例支援**：採用動態分配的 Consumer Name，支援後端服務橫向擴展，確保在高併發環境下的訊息處理能力。
- **分散式鎖**：利用 Redis 實作全局請求鎖，防止單一使用者同時發送多個重複提問 (Idempotency Guard)。

### 💬 優質對話體驗
- **SSE 流式回覆**：AI 回饋即時呈現，避免長時間等待。
- **即時排隊追蹤**：當系統繁忙時，前端會顯示即時排隊位置，並提供具備冷卻時間的「手動刷新」功能。
- **任務取消**：提供 `/stop` 端點，讓使用者能隨時中斷正在處理的請求。

### 🧩 模組化設計
- **AnythingLLM 整合**：支援自定義知識庫與 RAG 流程。
- **MCP Server 支援**：支援 Model Context Protocol 工具整合與日誌紀錄。

---

## 📖 快速開始

1. **環境設定**：複製 `.env.example` 並設定 `REDIS_HOST`, `DB_URL` 及 `ANYTHING_LLM_API_KEY`。
2. **啟動後端**：執行 `mvn spring-boot:run`。
3. **啟動前端**：直接透過 Web Server 開啟 `frontend/index.html`。

---

> [!TIP]
> 關於更深入的系統設計與訊息流轉邏輯，請參考 [ARCHITECTURE.md](./ARCHITECTURE.md)。

