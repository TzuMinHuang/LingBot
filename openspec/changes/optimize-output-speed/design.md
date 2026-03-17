# Design: Dynamic Typewriter Speed

## Mechanism
為了平衡視覺動態與串流即時性，前端打字機將根據隊列積壓程度動態調整速度。

### Thresholds
- **Normal**: Queue length < 50 chars -> Use `config.streamSpeed` (10ms).
- **Fast**: Queue length >= 50 chars -> Use `5ms`.
- **Turbo**: Queue length >= 150 chars -> Use `0ms` (Direct render).

### Workflow
1. `_drainQueue` 每個 tick 檢查隊列長度。
2. 根據長度計算當次 `sleep` 時間。
3. 若隊列內容過多，則一次消耗多個字元或減少延遲。
