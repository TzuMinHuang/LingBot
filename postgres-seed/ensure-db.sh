#!/bin/bash
set -e

# 使用環境變數連線（由 docker-compose 傳入）
export PGHOST=postgres
export PGPORT=5432
export PGUSER="${POSTGRES_USER}"
export PGPASSWORD="${POSTGRES_PASSWORD}"

echo "[ensure-db] Creating databases (if not exist)..."
for db in chatdb anythingllm_db litellm_db; do
  psql -d template1 -tc "SELECT 1 FROM pg_database WHERE datname='$db'" | grep -q 1 \
    || psql -d template1 -c "CREATE DATABASE $db;"
  echo "  ✓ $db"
done

echo "[ensure-db] Creating roles (if not exist)..."
create_role() {
  local role=$1 pass=$2
  psql -d template1 -tc "SELECT 1 FROM pg_roles WHERE rolname='$role'" | grep -q 1 \
    || psql -d template1 -c "CREATE ROLE $role WITH LOGIN PASSWORD '$pass';"
  # 確保密碼與 .env 同步
  psql -d template1 -c "ALTER ROLE $role WITH PASSWORD '$pass';"
  echo "  ✓ $role"
}

create_role "${DB_USERNAME}" "${DB_PASSWORD}"
create_role "${ANYTHINGLLM_DB_USER}" "${ANYTHINGLLM_DB_PASSWORD}"
create_role "${LITELLM_DB_USER}" "${LITELLM_DB_PASSWORD}"

echo "[ensure-db] Granting privileges..."
psql -d template1 <<SQL
REVOKE CONNECT ON DATABASE chatdb FROM PUBLIC;
REVOKE CONNECT ON DATABASE anythingllm_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE litellm_db FROM PUBLIC;

GRANT ALL PRIVILEGES ON DATABASE chatdb TO ${DB_USERNAME};
GRANT ALL PRIVILEGES ON DATABASE anythingllm_db TO ${ANYTHINGLLM_DB_USER};
GRANT ALL PRIVILEGES ON DATABASE litellm_db TO ${LITELLM_DB_USER};
SQL

psql -d chatdb -c "GRANT ALL ON SCHEMA public TO ${DB_USERNAME};"
psql -d anythingllm_db -c "GRANT ALL ON SCHEMA public TO ${ANYTHINGLLM_DB_USER};"
psql -d litellm_db -c "GRANT ALL ON SCHEMA public TO ${LITELLM_DB_USER};"

echo "[ensure-db] Creating pgvector extension..."
for db in chatdb anythingllm_db litellm_db; do
  psql -d "$db" -c "CREATE EXTENSION IF NOT EXISTS vector;"
  echo "  ✓ $db"
done

echo "[ensure-db] Done ✓"