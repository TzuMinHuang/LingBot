#!/bin/sh
set -e

# 等待 AnythingLLM 就緒
echo "[ensure-settings] Waiting for AnythingLLM..."
until curl -sf http://anythingllm:3001/api/v1/system -H "Authorization: Bearer ${ANYTHINGLLM_API_KEY}" > /dev/null 2>&1; do
  sleep 3
done
echo "[ensure-settings] AnythingLLM is ready"

# 透過 API 設定 LLM / Embedding / VectorDB
echo "[ensure-settings] Applying provider settings..."
HTTP_CODE=$(curl -s -o /tmp/response.json -w "%{http_code}" -X POST \
  -H "Authorization: Bearer ${ANYTHINGLLM_API_KEY}" \
  -H "Content-Type: application/json" \
  -d "{
    \"LLMProvider\": \"generic-openai\",
    \"GenericOpenAiBasePath\": \"${LITELLM_BASE_URL}\",
    \"GenericOpenAiModelPref\": \"${LLM_MODEL}\",
    \"GenericOpenAiKey\": \"${LITELLM_MASTER_KEY}\",
    \"EmbeddingEngine\": \"generic-openai\",
    \"EmbeddingBasePath\": \"${LITELLM_BASE_URL}\",
    \"EmbeddingModelPref\": \"${EMBEDDING_MODEL}\",
    \"GenericOpenAiEmbeddingApiKey\": \"${LITELLM_MASTER_KEY}\",
    \"EmbeddingModelMaxChunkLength\": \"8192\",
    \"GenericOpenAiTokenLimit\": \"4096\",
    \"VectorDB\": \"pgvector\",
    \"PGVectorConnectionString\": \"${PGVECTOR_CONNECTION_STRING}\"
  }" \
  "http://anythingllm:3001/api/v1/system/update-env")

if [ "$HTTP_CODE" -eq 200 ]; then
  echo "[ensure-settings] Settings applied successfully ✓"
  cat /tmp/response.json
  echo ""
else
  echo "[ensure-settings] Failed (HTTP $HTTP_CODE):"
  cat /tmp/response.json
  echo ""
  exit 1
fi