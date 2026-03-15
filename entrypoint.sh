#!/bin/bash
set -e

echo "=== 啟動基礎設施 ==="
docker compose --profile infra up -d

# 等待 init 容器完成
echo "=== 等待初始化完成 ==="
docker wait postgres-init ollama-init anythingllm-init 2>/dev/null || true

# 清理已停止的 init 容器
docker rm postgres-init ollama-init anythingllm-init 2>/dev/null || true

echo "=== 啟動應用層 ==="
docker compose --profile infra --profile app up -d --build

echo "=== 完成 ✓ ==="