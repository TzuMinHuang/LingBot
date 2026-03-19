## 1. PendingTaskScanner ack 參數修正

- [x] 1.1 修正 `PendingTaskScanner.moveToDlqAndAck()` 中 `.acknowledge(consumerGroup, streamKey, recordId)` 的參數順序為 `.acknowledge(streamKey, consumerGroup, recordId)`

## 2. Docker Compose 水平擴展修正

- [x] 2.1 移除 `docker-compose.yml` 中 `backend-service` 的 `container_name: backend-service`

## 3. SseSessionManager 資源洩漏修正

- [x] 3.1 在 `SseSessionManager.drainQueue()` 加入 `finally { remove(sessionId); }` 確保線程結束時清理 session 資源
