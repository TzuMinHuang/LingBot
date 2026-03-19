## ADDED Requirements

### Requirement: Official Docker Configuration Pattern
AnythingLLM Docker 部署 SHALL 遵循官方推薦模式：透過掛載 `.env` 檔案至 `/app/server/.env` 管理應用設定，透過 `DATABASE_URL` environment variable 指定 PostgreSQL 連線。

#### Scenario: Container starts with mounted .env
- **WHEN** AnythingLLM container 啟動且 `./data/anythingllm/.env` 已掛載至 `/app/server/.env`
- **THEN** AnythingLLM SHALL 讀取該檔案中的 LLM/Embedding/VectorDB 設定，無需額外的 init container

#### Scenario: No init container dependency
- **WHEN** docker-compose infra profile 啟動完成
- **THEN** AnythingLLM SHALL 在無 `anythingllm-init` container 的情況下正確運行所有功能
