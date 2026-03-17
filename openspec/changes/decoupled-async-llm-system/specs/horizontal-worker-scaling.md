## ADDED Requirements

### Requirement: 基於隊列深度的 HPA
系統必須暴露 Redis Stream `stream:bot:req` 的 Pending 數量作為指標。

#### Scenario: 流量高峰自動擴容
- **WHEN** 隊列積壓任務數量超過 50 個
- **THEN** Kubernetes 應該啟動新的 `llm-worker` Pod 以加速處理

### Requirement: 任務申領與重試 (XCLAIM)
Worker 定時檢查 PEL (Pending Entries List)，若任務處理超時則重新申領。

#### Scenario: Worker 崩潰任務回收
- **WHEN** Worker A 在處理任務途中崩潰且未 ACK
- **THEN** Worker B 在 30 秒後應能透過 `XCLAIM` 重新獲取並處理該任務
