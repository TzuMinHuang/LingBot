-- 建立應用所需的資料庫（docker-entrypoint-initdb.d 在 postgres DB 上下文執行）
-- chatdb:         LingBot 應用資料庫
-- anythingllm_db: AnythingLLM 專屬
-- litellm_db:     LiteLLM 專屬

SELECT 'CREATE DATABASE chatdb'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'chatdb')\gexec

SELECT 'CREATE DATABASE anythingllm_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'anythingllm_db')\gexec

SELECT 'CREATE DATABASE litellm_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'litellm_db')\gexec

-- ── 建立各服務專屬帳號 ──────────────────────────────────────

-- LingBot backend
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'chatuser') THEN
        CREATE ROLE chatuser WITH LOGIN PASSWORD 'chatpass';
    END IF;
END $$;

-- AnythingLLM
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'anythingllm_user') THEN
        CREATE ROLE anythingllm_user WITH LOGIN PASSWORD 'anythingllm_pass';
    END IF;
END $$;

-- LiteLLM
DO $$ BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'litellm_user') THEN
        CREATE ROLE litellm_user WITH LOGIN PASSWORD 'litellm_pass';
    END IF;
END $$;

-- ── 授權：每個帳號只能存取自己的資料庫 ─────────────────────

-- 先撤銷 public schema 的預設連線權限（PG15+ 預設已無，但保險起見）
REVOKE CONNECT ON DATABASE chatdb FROM PUBLIC;
REVOKE CONNECT ON DATABASE anythingllm_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE litellm_db FROM PUBLIC;

GRANT ALL PRIVILEGES ON DATABASE chatdb TO chatuser;
GRANT ALL PRIVILEGES ON DATABASE anythingllm_db TO anythingllm_user;
GRANT ALL PRIVILEGES ON DATABASE litellm_db TO litellm_user;

-- ── 在各資料庫內授予 schema 權限 ─────────────────────────────
-- (docker-entrypoint-initdb.d 的 .sql 只在預設 DB 執行，
--  需用 \c 切換到目標 DB 才能授權 schema 層級)

\c chatdb
GRANT ALL ON SCHEMA public TO chatuser;

\c anythingllm_db
GRANT ALL ON SCHEMA public TO anythingllm_user;

\c litellm_db
GRANT ALL ON SCHEMA public TO litellm_user;
