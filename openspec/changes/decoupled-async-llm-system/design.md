## Context

目前的系統架構中，API Server 與 LLM 處理邏輯緊密耦合，且在單一處理執行緒中同步等待 LLM 產出（使用 `blockLast()`），這導致系統難以應對突發流量，且單點故障風險高。

## Goals / Non-Goals

**Goals:**
- **解耦**：將請求接收 (Ingress) 與 LLM 推理 (Worker) 物理分離。
- **高併發**：利用 Reactive Programming (WebFlux) 提升單機吞吐量。
- **可靠性**：支持訊息確認 (ACK)、重試與斷線重連數據補發。
- **伸縮性**：基於 Redis Stream 深度實現 K3s 自動擴展。

**Non-Goals:**
- **長期存儲重構**：不改變 PostgreSQL/Redis 的基本數據模型。
- **模型優化**：不涉及 LLM 模型本身的推理性能優化。

## Decisions

### 1. Inbound Request Pipeline: Redis Streams + Consumer Group
- **決策**：棄用簡單的 Redis List，改用 `Redis Streams` 搭配 `Consumer Group`。
- **理由**：
  - **負載均衡**：Consumer Group 自動在多個 Worker 間分配任務。
  - **可靠性**：透過 Pending Entries List (PEL) 確保 Worker 崩潰後訊息可被重新申領 (XCLAIM)。
  - **可視化**：易於監控隊列堆積情況。

### 2. Outbound Response Pipeline: Dual-Mode (Pub/Sub + Stream)
- **決策**：同時啟動兩種模式。
  - **Mode A (SSE over Pub/Sub)**：前端建立 SSE 請求後，後端透過 Redis Pub/Sub 廣播 Token。優點是延遲極低。
  - **Mode B (Session Backup Stream)**：Worker 同時將 Token 寫入 `stream:chat:res:{sessionId}`。
- **理由**：
  - **重連支持**：若 SSE 斷開，前端重連時可帶上 Last-Event-ID，API Server 從 Session Stream 中讀取遺漏的 Token。

### 3. Service Decomposition
- **API Server (Reactive)**：負責 Auth、驗證、連線管理（SSE/WS）及 `XADD` 請求。
- **LLM Worker**：負責消費 `stream:bot:req`，呼叫 LiteLLM，並雙寫回傳結果。

## Risks / Trade-offs

- **[Risk] Redis 負載增加** → **Mitigation**: 實作 `MAXLEN` 策略自動修剪 Session Stream，並對 Pub/Sub 頻道進行 TTL 管理。
- **[Risk] 亂序問題** → **Mitigation**: 請求與響應均帶有遞增的序列號 (Sequence Number) 或 Timestamp，前端負責排序。
- **[Trade-off] 系統複雜度** → 換取更高的穩定性與彈性。

## Infrastructure (K3s) Strategy

- **HPA**: 使用 `Prometheus Adapter` 監控 Redis `XPENDING` 數量。當累積過多請求時，自動水平擴展 `llm-worker` Pod。
- **Liveness Probe**: 檢查 Worker 與 Redis 的連線心跳，而非僅檢查進程。
