#!/bin/bash
set -e

MODEL_FILE="/models.txt"

echo "[ensure-models] Checking models..."

# 取得已安裝的模型清單
installed=$(ollama list 2>/dev/null | awk 'NR>1{print $1}')

while IFS= read -r model || [ -n "$model" ]; do
  # 跳過空行和註解
  model=$(echo "$model" | xargs)
  [[ -z "$model" || "$model" == \#* ]] && continue

  # 檢查是否已安裝
  if echo "$installed" | grep -q "^${model}"; then
    echo "  ✓ $model (already installed)"
  else
    echo "  ↓ $model (downloading...)"
    ollama pull "$model"
    echo "  ✓ $model (downloaded)"
  fi
done < "$MODEL_FILE"

echo "[ensure-models] Done ✓"