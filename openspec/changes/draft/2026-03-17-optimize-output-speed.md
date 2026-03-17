# Fix Proposal: Optimize Slow Typewriter Output

## Context
使用者回報 AI 輸出速度過慢。經分析後發現：
1. **原因**：前端 `ChatUI.js` 使用打字機效果 (`_drainQueue`)，預設延遲為每字 18ms。
2. **現象**：當 LLM 傳回速度超過每秒 55 字時，前端打字機隊列會不斷積壓，導致使用者感覺輸出滯後，且在串流結束後仍需等待打字機跑完。

## Proposed Changes

### Frontend Implementation
#### [MODIFY] [ChatUI.js](file:///Users/j/web/chatbot/LingBot/frontend/js/ChatUI.js)
- 修改 `_drainQueue` 邏輯，加入動態調速機制。
- 當 `_streamQueue` 長度超過閾值（例如 50 字）時，將 `_streamSpeed` 調降（例如設為 0ms 或 5ms），以快速消耗積壓內容。
- 當隊列清空後，恢復原有的 `streamSpeed` 設置。

#### [MODIFY] [env.json](file:///Users/j/web/chatbot/LingBot/frontend/config/env.json)
- 將預設 `streamSpeed` 從 18ms 調整為更流暢的 10ms。

## Tasks

- [ ] 修改 `ChatUI.js` 實作動態調速打字機 [ ]
- [ ] 調整 `env.json` 的預設 `streamSpeed` [ ]
- [ ] 驗證在大段文字輸出時，前端是否能及時跟上串流速度 [ ]
