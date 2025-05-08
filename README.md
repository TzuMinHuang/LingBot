# 🤖 Linbot — 對話式流程管理引擎 (重構中)

**Linbot** 是一個基於Rasa 與 Spring Boot 為基底，結合自製中的 FlowMate 架構的對話式流程驅動系統，支援 Plugin 式模組化擴展，可靈活實現單輪 / 多輪對話流程、客服轉接與條件控制，適合用於客服機器人、語音助理或任務導向導覽系統等場景。

---

## 🧰 開發框架與語言

- 後端：Java 17+、Spring Boot
- 前端：html + css +javascript

## 🗃 資料持久化支援
- 流程資料儲存：PostgreSQL
- 儲存對話上下文、Token、暫存狀態等：Redis
- 非同步消息處理：RabbitMQ

☁️ 架構與工具
- 容器化支援：Docker 化部署
- 版本控制：Git + GitHub

## 🔧 核心架構

### ✅ Rasa NLU
### ✅ FlowMate 流程引擎

- `ProcessEngine`：流程執行引擎，負責整體流程控制與指令派發。
- `ProcessInstance`：流程實例，表示一個執行中的流程實例。
- `ProcessContext`：流程上下文資料載體，保存執行狀態、輸入參數、結果等。
- `ProcessTemplate`：流程節點組合的藍圖，支援多種流程設計。
- `ProcessRegistry`：註冊與管理所有流程模板。
- `StepCommand`：可重用的節點指令邏輯單元。
- `StepCommandSimple`:簡單步驟命令，執行具體步驟操作的指令。
- `StepCommandRunCondition`：條件執行機制，支援邏輯判斷、策略過濾。
- `StepStrategy`: 步驟策略，用於定義流程中每個步驟的處理邏輯。

---
## 相關功能

### 🧩 Plugin 式對話模組

模組化設計，每個 Plugin 對應獨立流程，可動態註冊與插拔，實現「功能解耦 + 對話流程封裝」。

---
### 💬 對話支援

#### 🟢 單輪對話（SingleTurnDialogue）
- 適用於 FAQ、打招呼、靜態問答等無狀態回應
- 例如：「營業時間是幾點？」→「早上9點到晚上6點」

#### 🔵 多輪對話（MultiTurnDialogue）
- 任務導向 / 條件流程 / 輸入引導
- 例如：「我要預約」→「請問哪一天？」→「幾點？」

#### 👥 客服轉接
- 可設定特定流程節點觸發轉真人客服
- 支援條件式轉接（多次失敗、自訂條件等）

---

