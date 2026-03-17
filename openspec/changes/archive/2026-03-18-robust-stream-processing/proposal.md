## Why

背景訊息復原機制 `PendingTaskScanner` 存在 `ClassCastException` 錯誤。這導致系統無法正常收回超時未確認（ACK）的訊息，且該錯誤會阻塞 Reactive Event Loop，使 Actuator Health Check 失效，最終導致容器被重啟或無法連線。

## What Changes

- **修復轉型錯誤**: 在 `PendingTaskScanner.java` 中，將 `Mono<PendingMessages>` 正確地轉換為 `Flux<PendingMessage>`，避免 `ClassCastException`。
- **優化復原邏輯**: 改進 `claimMessage` 的處理流程，確保非同步操作的串接順暢。
- **異常處理強化**: 在背景任務中加入更詳細的錯誤日誌與異常攔截，防止單一訊息處理失敗影響整個掃描週期。

## Capabilities

### New Capabilities
- `stream-reliability`: 提供更穩健的 Redis Stream 訊息補償與錯誤監控機制。

## Impact

- **Affected Code**: `PendingTaskScanner.java`, `BotConsumer.java`
- **Infrastructure**: Redis Stream 消費者組狀態與訊息堆積。
