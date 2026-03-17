## 1. Audit & Preparation

- [x] 1.1 檢查 `PendingTaskScanner.java` 的轉型邏輯與 `flatMapMany` 使用情形
- [x] 1.2 稽核 `BotConsumer.java` 是否有類似的 Reactive Redis 轉型陷阱

## 2. Implementation

- [x] 2.1 修復 `PendingTaskScanner.java` 中對 `Mono<PendingMessages>` 的處理
- [x] 2.2 優化錯誤捕捉，將 Exception 類別名稱加入日誌以利排錯
- [x] 2.3 確保 `claimMessage` 在訊息已不存在時能優雅返回

## 3. Verification

- [x] 3.1 驗證 `backend-service` 在 Docker 環境中處於 `healthy` 狀態
- [x] 3.2 觀察日誌確認積壓訊息 (約 900+ 筆) 是否已成功被收回並處理
- [x] 3.3 手動觸發 `/actuator/health` 確保響應時間正常
