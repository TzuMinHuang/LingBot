# Fix Proposal: Persistent Queuing Status Issue

## Context
使用者回報前台界面一直顯示「排隊中」或錯誤的排隊人數。經過分析發現：
1. `ChatBotController.queuePosition` 使用 `redisTemplate.opsForStream().size()` (即 `XLEN`) 來計算排隊位置。
2. 由於專案未對 Redis Stream (`stream:bot:incoming`) 進行過修剪 (Trimming)，`XLEN` 會隨訊息總量無限制增長。
3. 排隊計算公式 `streamLen - consumers` 在訊息累積後會算出極大的數值，導致前端誤以為正在排隊。

## Proposed Changes

### Backend Architecture
#### [MODIFY] [ChatBotController.java](file:///Users/j/web/chatbot/LingBot/backend/src/main/java/idv/hzm/app/bot/controller/ChatBotController.java)
- 修改 `queuePosition` 方法，改用 Redis Stream 的 `lag` 屬性（透過 `XINFO GROUPS` 取得）。
- `lag` 代表尚未分配給任何消費者的訊息數量，最能準確反映真實的排隊長度。

#### [MODIFY] [BotConsumer.java](file:///Users/j/web/chatbot/LingBot/backend/src/main/java/idv/hzm/app/bot/consumer/BotConsumer.java)
- 在訊息處理完成並 Acknowledge 後，加入 Stream 修剪邏輯。
- 使用 `XTRIM` 保留最近的 100 筆紀錄，防止 Redis 記憶體過度消耗。

## Tasks

- [x] 修改 `ChatBotController.java` 以獲取 Redis Stream `lag` [ ]
- [x] 在 `BotConsumer.java` 加入 `XTRIM` 邏輯以限制 Stream 大小 [ ]
- [x] 驗證排隊人數是否在訊息處理後歸零 [ ]
