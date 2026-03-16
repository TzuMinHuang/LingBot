import time
import json
from locust import HttpUser, task, between, events

class LingBotUser(HttpUser):
    """
    Locust 測試腳本：專為 LingBot SSE 流程設計
    流程：
    1. POST /bot/chat/initial -> 取得 sessionId
    2. GET /bot/chat/{sessionId}/stream -> 開啟 SSE 連線
    3. POST /bot/chat/{sessionId}/send -> 發送內容
    """
    
    wait_time = between(2, 5)

    def on_start(self):
        """初始化：確保每個 User 都有 sessionId"""
        with self.client.post("/bot/chat/initial", name="01_Init_Session") as response:
            if response.status_code == 200:
                self.session_id = response.json().get("sessionId")
            else:
                self.session_id = None
                print(f"Failed to initialize session: {response.status_code}")

    @task
    def chat_flow(self):
        if not self.session_id:
            return

        # 準備路徑
        stream_url = f"/bot/chat/{self.session_id}/stream"
        send_url = f"/bot/chat/{self.session_id}/send"
        
        payload = {"content": "請寫一個關於 Ollama 的 50 字簡介。"}
        
        start_time = time.time()
        first_token_time = None
        total_tokens = 0
        
        try:
            # 1. 開啟 SSE 串流連線
            with self.client.get(stream_url, stream=True, name="02_SSE_Stream") as sse_resp:
                if sse_resp.status_code != 200:
                    return
                
                # 2. 發送訊息
                with self.client.post(send_url, json=payload, name="03_Send_Message") as send_resp:
                    if send_resp.status_code != 200:
                        return

                # 3. 解析串流內容
                for line in sse_resp.iter_lines():
                    if not line:
                        continue
                    
                    line_str = line.decode('utf-8')
                    if line_str.startswith("data:"):
                        try:
                            # LingBot 格式: data: {"type":"STREAM_CHUNK","content":"...","sessionId":"..."}
                            data = json.loads(line_str[5:])
                            msg_type = data.get("type")
                            
                            if msg_type == "STREAM_CHUNK":
                                total_tokens += 1
                                if first_token_time is None:
                                    first_token_time = time.time()
                                    # TTFT: 從發送 request 到第一個 chunk
                                    ttft = (first_token_time - start_time) * 1000
                                    events.request.fire(
                                        request_type="SSE_METRIC",
                                        name="Queueing Latency (TTFT)",
                                        response_time=ttft,
                                        response_length=0
                                    )
                            
                            elif msg_type == "STREAM_END":
                                end_time = time.time()
                                total_duration = (end_time - start_time) * 1000
                                
                                # TPS: Tokens Per Second
                                gen_time = end_time - first_token_time if first_token_time else 0
                                tps = total_tokens / gen_time if gen_time > 0 else 0
                                
                                events.request.fire(
                                    request_type="SSE_METRIC",
                                    name="Token Generation Speed (TPS)",
                                    response_time=tps,
                                    response_length=total_tokens
                                )
                                break
                                
                        except Exception:
                            pass
                            
        except Exception as e:
            events.request.fire(
                request_type="SSE_ERROR",
                name="Connection Error",
                response_time=0,
                response_length=0,
                exception=e
            )
