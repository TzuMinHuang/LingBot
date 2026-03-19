## Context

AnythingLLM 目前在 docker-compose 中以大量 environment variables 配置，並搭配一個 `anythingllm-init` curl container 透過 REST API 補充無法透過 env var 設定的項目。官方推薦做法是將 `.env` 檔案直接掛載至 `/app/server/.env`，讓 AnythingLLM 啟動時直接讀取。

現有 storage volume 將 `./data/anythingllm` 整個目錄掛載至 `/app/server/storage`，改造後需將 `.env` 和 storage 分開管理。

## Goals / Non-Goals

**Goals:**
- 對齊官方 Docker 部署模式（`.env` 掛載 + `DATABASE_URL` env var）
- 移除 `anythingllm-init` container，簡化啟動流程
- 所有 AnythingLLM 應用設定集中在 `./data/anythingllm/.env`

**Non-Goals:**
- 不變更 AnythingLLM 的 LLM/Embedding/VectorDB provider 選擇
- 不變更 PostgreSQL 資料庫結構或連線方式
- 不處理 secrets 管理（明文 `.env` 為官方做法，暫不引入 vault）
- 不變更 backend-service 對 AnythingLLM 的呼叫方式

## Decisions

### D1: `.env` 檔案掛載方式

採用官方做法，將 `./data/anythingllm/.env` 掛載至 `/app/server/.env`。

**替代方案：** 維持 Docker env vars → 但無法覆蓋所有設定（如 `GenericOpenAiTokenLimit`），需要 init container 補充。

### D2: 設定分層 — docker-compose env vs .env 檔案

| 層 | 內容 | 原因 |
|---|------|------|
| docker-compose `environment` | `STORAGE_DIR`, `DATABASE_URL` | 與 compose 網路拓撲相關的基礎設施級設定 |
| `./data/anythingllm/.env` | LLM/Embedding/VectorDB/JWT/Token Limit | AnythingLLM 應用級設定 |

**替代方案：** 全部放 `.env` → 但 `DATABASE_URL` 需要引用 compose 變數（`postgres:5432`），放在 compose 層較自然。

### D3: Storage 路徑調整

從 `./data/anythingllm:/app/server/storage` 改為：
- `./data/anythingllm/storage:/app/server/storage`
- `./data/anythingllm/.env:/app/server/.env`

這樣 `.env` 和 storage 在 host 上同一父目錄但互不干擾。

### D4: 新增 `cap_add: SYS_ADMIN`

官方推薦加上，用於 AnythingLLM 內部的文件處理（如 PDF 解析）。

### D5: `.env` 檔案管理

- 建立 `anythingllm/.env.example` 作為範本（納入版本控制）
- `./data/anythingllm/.env` 為實際運行檔案（不納入版本控制，已在 `.gitignore`）
- 部署時需手動從範本複製並填入 secrets

## Risks / Trade-offs

- **[風險] 現有 storage 資料遷移** → 路徑從 `./data/anythingllm/` 變為 `./data/anythingllm/storage/`，需遷移現有上傳文件。Mitigation: 在 tasks 中加入遷移步驟說明。
- **[風險] `.env` 檔案遺漏設定** → 從 docker-compose env vars + init script 彙整時可能漏項。Mitigation: 對照現有 env vars 和 init script 逐項檢查。
- **[取捨] Secrets 明文** → 官方做法就是明文 `.env`，與現有 root `.env` 的安全等級相同，可接受。
