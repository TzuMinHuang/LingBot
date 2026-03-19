## 1. 基礎設施與配置 (Infrastructure & Configuration)

- [x] 1.1 更新 Maven 依賴，引入 `spring-boot-starter-webflux` 與 `spring-boot-starter-data-redis-reactive`
- [x] 1.2 設定 `ReactiveRedisTemplate` 與 `StreamReceiver` 配置類
- [x] 1.3 實作 Redis Stream 初始化腳本（確保 Group `bot-consumers` 存在）

## 2. API Server (Ingress) 實作

- [x] 2.1 重構 `ChatBotController` 為非阻塞式存取，實作 `XADD` 任務推送
- [x] 2.2 實作 SSE 雙模連線邏輯：同時訂閱 Pub/Sub 並從 Session Stream 讀取歷史
- [x] 2.3 實作 `SseEmitterManager` 的反應式版本

## 3. LLM Worker 實作

- [x] 3.1 實作 `BotConsumer` 作為 Redis Stream 消費者
- [x] 3.2 整合 `AnythingLLMClient` 並實作雙模輸出 (Pub/Sub + Session Stream)
- [x] 3.3 實作分層錯誤處理與 `XACK` 確認機制
- [ ] 3.4 實作任務確認碼 `XACK` 邏輯

## 4. 可靠性與錯誤處理 (Reliability & Error Handling)

- [ ] 4.1 實作基於 `interactionId` 的請求冪等緩存
- [x] 4.1 實作 `PendingTaskScanner` 使用 `XCLAIM` 恢復超時任務
- [x] 4.2 實作 Dead Letter Queue (DLQ) 處理多次失敗任務
邏輯，處理 Poison Pill 訊息

## 5. K3s 部署與優化 (Deployment & Optimization)

- [ ] 5.1 撰寫 Dockerfile，區分 API 與 Worker 的啟動 Profile
- [ ] 5.2 撰寫 Kubernetes Deployment 與 Service 資源清單
- [ ] 5.3 配置 HPA (Horizontal Pod Autoscaler) 基於 Redis Stream 深度
