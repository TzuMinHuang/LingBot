## Why

Code review 發現 backend 架構重構後殘留 3 個需立即修復的問題：一個真 bug（PendingTaskScanner ack 參數順序反了）、一個部署阻礙（container_name 阻擋水平擴展）、一個資源洩漏（SSE drain 線程結束後未清理 session）。這些問題在生產環境會造成 pending message 無法清除、Docker scale 失敗、記憶體洩漏。

## What Changes

- 修正 `PendingTaskScanner.moveToDlqAndAck()` 中 `acknowledge()` 的參數順序（streamKey 與 consumerGroup 對調）
- 移除 `docker-compose.yml` 中 `backend-service` 的 `container_name`，允許 `--scale` 擴展
- 在 `SseSessionManager.drainQueue()` 加入 finally block 確保 session 資源清理

## Capabilities

### New Capabilities

（無新增）

### Modified Capabilities

- `stream-reliability`: 修正 pending message recovery 的 ack 行為，確保 DLQ 流程正確完成

## Impact

- `backend/src/main/java/idv/hzm/app/bot/consumer/PendingTaskScanner.java` — ack 參數修正
- `backend/src/main/java/idv/hzm/app/bot/config/SseSessionManager.java` — drainQueue 資源清理
- `docker-compose.yml` — backend-service container_name 移除
