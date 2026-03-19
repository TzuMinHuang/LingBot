## Context

Backend 架構從單一 `BotConsumer` 重構為 `RequestConsumer` / `ResponseConsumer` / `SseSessionManager` 三層架構後，code review 發現 3 個問題需修正。這些都是小範圍、低風險的 bugfix，不涉及架構變更。

## Goals / Non-Goals

**Goals:**
- 修正 PendingTaskScanner 的 ack 參數錯誤，確保 DLQ 後 pending message 能被正確清除
- 移除阻礙水平擴展的 container_name
- 消除 SSE drain 線程異常結束後的 session 資源洩漏

**Non-Goals:**
- 不處理 `chunkRepository.save()` 在 synchronized 內的效能問題（單行 INSERT 通常 <1ms，觀察即可）
- 不處理 `recoverFromLastEventId` 的順序邊界問題（斷線重連本身是 best-effort，BlockingQueue FIFO 已提供基本保證）

## Decisions

### 1. PendingTaskScanner ack 參數修正

`ReactiveStreamOperations.acknowledge()` 簽章為 `(K key, String group, RecordId...)`。現行程式碼 `acknowledge(consumerGroup, streamKey, recordId)` 把 group 和 key 對調了。直接交換參數即可。

### 2. docker-compose container_name 移除

只移除 `backend-service` 的 `container_name: backend-service`。其他 infra 服務（postgres, redis 等）保留 container_name，因為它們不需要 scale 且其他服務透過 container name 互相引用。

### 3. SseSessionManager drainQueue 清理

在 `drainQueue` 方法中加入 `finally { remove(sessionId); }`。`remove()` 已是冪等操作（ConcurrentHashMap.remove + tryEmitComplete），與 `doOnCancel` 的 remove 呼叫不會衝突。

## Risks / Trade-offs

- **[Risk] drainQueue finally 與 doOnCancel 重複呼叫 remove()** → `remove()` 是冪等的，ConcurrentHashMap.remove 對不存在的 key 回傳 null，Sink.tryEmitComplete 對已完成的 sink 是 no-op。無風險。
