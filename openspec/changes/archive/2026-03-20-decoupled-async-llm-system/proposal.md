## Why

目前的聊天機器人後端採用單體架構且存在阻塞式調用（如 `blockLast()`），導致在高併發情況下效能低下且擴展性受限。此外，缺乏可靠的訊息重傳機制，一旦客戶端斷線，正在產出的 Token 將會丟失。為了實現生產等級的穩定性與可擴展性，需要將系統解耦並導入基於 Redis 的非同步管線。

## What Changes

本變更將現有系統拆分為 **API 服務** 與 **LLM Worker 服務**：
1. **請求隊列**：API 接收使用者輸入後透過 `XADD` 寫入 Redis Stream，多個 Worker 透過 `XREADGROUP` 競爭消費。
2. **響應管線**：實作雙模輸出：
   - **Mode A (Pub/Sub)**：用於極低延遲的即時 Token 串流。
   - **Mode B (Session Stream)**：為每個對話維護一個 Redis Stream，支援重連後的數據回放。
3. **可靠性增強**：導入 `XACK`、`XPENDING` 重試機制、冪等性檢查與死信隊列 (DLQ)。

## Capabilities

### New Capabilities
- `async-request-ingestion`: 使用 Redis Streams 實現的非同步請求隊列，支援削峰填谷。
- `reliable-streaming-response`: 基於 Session Stream 的可靠響應機制，支援斷線重連數據補發。
- `horizontal-worker-scaling`: 支援在 K3s 環境下根據隊列深度自動伸縮 Worker 節點。

### Modified Capabilities
- `chatbot-core`: 核心處理邏輯由同步改為完全非同步與反應式結構。

## Impact

- **API 服務**：職責轉變為連線管理與請求分發，CPU 佔用將降低。
- **LLM Worker**：成為獨立的可擴展單元，負責 I/O 密集型的 LLM 呼叫。
- **Redis**：成為系統的核心中繼點，負載將會增加，需確保 HA 配置。
- **Kubernetes**：需新增進程間通信的配置與 HPA 策略。
