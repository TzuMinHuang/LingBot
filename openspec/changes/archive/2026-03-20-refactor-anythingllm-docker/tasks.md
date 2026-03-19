## 1. 建立 AnythingLLM .env 檔案

- [x] 1.1 建立 `anythingllm/.env.example` 範本，彙整所有設定項（LLM/Embedding/VectorDB/JWT/TokenLimit）
- [x] 1.2 建立 `./data/anythingllm/.env` 實際運行檔案（從現有 docker-compose env vars + ensure-settings.sh 彙整）
- [x] 1.3 確認 `.gitignore` 包含 `data/` 目錄

## 2. 重寫 docker-compose.yml anythingllm service

- [x] 2.1 重寫 anythingllm service：新增 `.env` 掛載、調整 storage 路徑、新增 `cap_add: SYS_ADMIN`、僅保留 `STORAGE_DIR` 和 `DATABASE_URL` 兩個 env vars
- [x] 2.2 移除 `anythingllm-init` service 區塊（L168-186）

## 3. 清理舊檔案

- [x] 3.1 刪除 `anythingllm/ensure-settings.sh`
- [x] 3.2 清理 `.env.example` 中不再由 docker-compose 直接引用的 AnythingLLM 變數說明（如需要）

## 4. 文件與遷移說明

- [x] 4.1 在 `anythingllm/.env.example` 頂部加入使用說明註解（複製到 data 目錄、填入 secrets）
- [x] 4.2 確認 storage 路徑遷移步驟：現有 `./data/anythingllm/` 內容需移至 `./data/anythingllm/storage/`
