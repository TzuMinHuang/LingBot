## Why

AnythingLLM Docker 配置偏離官方推薦做法：缺少 `.env` 檔案掛載、依賴額外的 init container 透過 REST API 補設定、storage volume 結構未分離。改為官方模式可簡化啟動流程、減少活動 container 數量，並讓所有 AnythingLLM 設定集中管理。

## What Changes

- 移除 `anythingllm-init` service 及其 `ensure-settings.sh` 腳本
- 新增 `./data/anythingllm/.env` 掛載至 `/app/server/.env`（官方推薦做法）
- 將 storage volume 從 `./data/anythingllm` 調整為 `./data/anythingllm/storage`，與 `.env` 分離
- 將 docker-compose 中 anythingllm 的大量 environment variables 移入 `.env` 檔案，僅保留 `STORAGE_DIR` 和 `DATABASE_URL`
- 新增 `cap_add: SYS_ADMIN`（官方推薦）
- 新增 `GENERIC_OPEN_AI_TOKEN_LIMIT=4096` 設定（原本僅在 init script 中設定）

## Capabilities

### New Capabilities

（無 — 此變更為基礎設施重構，不涉及新的應用層能力）

### Modified Capabilities

（無 — 不影響任何現有 spec 層級的行為）

## Impact

- `docker-compose.yml`：anythingllm service 定義重寫、anythingllm-init service 移除
- `anythingllm/ensure-settings.sh`：刪除
- `./data/anythingllm/`：目錄結構變更（新增 `.env` 檔案、storage 子目錄）
- `.env` / `.env.example`：可能需要移除不再由 docker-compose 直接引用的 AnythingLLM 變數
- 現有 `./data/anythingllm/` 中的上傳文件需遷移至 `./data/anythingllm/storage/`
