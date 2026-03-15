"""
LingBot 壓力測試腳本
協定：SockJS + STOMP over WebSocket

執行方式：
  pip install locust websocket-client
  locust -f locustfile.py --host http://192.168.0.130

Web UI:  http://localhost:8089
"""

import json
import threading
import time
import uuid

import websocket
from locust import HttpUser, between, events, task


class ChatUser(HttpUser):
    wait_time = between(2, 5)  # 每次發問後等待 2~5 秒再發下一問

    # ── 初始化：建立 session + 連線 WebSocket ──────────────────
    def on_start(self):
        resp = self.client.post("/bot/chat/initial", name="POST /bot/chat/initial")
        if resp.status_code != 200:
            self.environment.runner.quit()
            return
        self.session_id = resp.json().get("sessionId")

        self._ttft       = None
        self._send_time  = None
        self._done_event = threading.Event()
        self._ws_ready   = threading.Event()
        self._connect_ws()
        self._ws_ready.wait(timeout=10)  # 等 STOMP CONNECTED

    def on_stop(self):
        if hasattr(self, "_ws") and self._ws:
            self._ws.close()

    # ── SockJS/STOMP WebSocket 連線 ────────────────────────────
    def _connect_ws(self):
        server_id  = "000"
        sock_sess  = uuid.uuid4().hex[:8]
        ws_url     = (
            f"ws://{self.host.replace('http://', '').replace('https://', '')}"
            f"/bot/ws/{server_id}/{sock_sess}/websocket"
        )

        self._ws = websocket.WebSocketApp(
            ws_url,
            on_open    = self._on_open,
            on_message = self._on_message,
            on_error   = self._on_error,
            on_close   = self._on_close,
        )
        t = threading.Thread(target=self._ws.run_forever, kwargs={"ping_interval": 30})
        t.daemon = True
        t.start()

    def _on_open(self, ws):
        connect = "CONNECT\naccept-version:1.1,1.0\nheart-beat:0,0\n\n\x00"
        ws.send(json.dumps([connect]))

    def _on_message(self, ws, raw):
        if raw == "o":       # SockJS open — 等待 STOMP CONNECTED
            return
        if raw == "h":       # SockJS heartbeat
            return
        if not raw.startswith("a"):
            return

        frames = json.loads(raw[1:])
        for frame in frames:
            self._handle_stomp(frame)

    def _handle_stomp(self, frame):
        if frame.startswith("CONNECTED"):
            sub = (
                f"SUBSCRIBE\nid:sub-0\n"
                f"destination:/topic/user/{self.session_id}/receive\n\n\x00"
            )
            self._ws.send(json.dumps([sub]))
            self._ws_ready.set()

        elif frame.startswith("MESSAGE"):
            header, _, body = frame.partition("\n\n")
            body = body.rstrip("\x00")
            try:
                msg = json.loads(body)
            except Exception:
                return

            if msg.get("type") == "STREAM_CHUNK":
                if self._ttft is None and self._send_time:
                    self._ttft = (time.time() - self._send_time) * 1000
                    events.request.fire(
                        request_type  = "WS",
                        name          = "TTFT (首個 Token 延遲)",
                        response_time = self._ttft,
                        response_length = 0,
                        exception     = None,
                    )

            elif msg.get("type") == "STREAM_END":
                if self._send_time:
                    total = (time.time() - self._send_time) * 1000
                    events.request.fire(
                        request_type  = "WS",
                        name          = "Total (完整回覆時間)",
                        response_time = total,
                        response_length = 0,
                        exception     = None,
                    )
                self._done_event.set()

    def _on_error(self, ws, error):
        events.request.fire(
            request_type  = "WS",
            name          = "WebSocket Error",
            response_time = 0,
            response_length = 0,
            exception     = error,
        )

    def _on_close(self, ws, code, msg):
        pass

    # ── 壓測任務：發送問題 ──────────────────────────────────────
    @task
    def send_message(self):
        if not self._ws_ready.is_set():
            return

        self._done_event.clear()
        self._ttft      = None
        self._send_time = time.time()

        payload = json.dumps({"type": "message", "content": "請問如何申請信用卡？"})
        send_frame = (
            f"SEND\n"
            f"destination:/app/chat/{self.session_id}/send\n"
            f"content-type:application/json\n\n"
            f"{payload}\x00"
        )
        self._ws.send(json.dumps([send_frame]))

        # 等待 STREAM_END，最長 3 分鐘（AnythingLLM timeout）
        finished = self._done_event.wait(timeout=180)
        if not finished:
            events.request.fire(
                request_type  = "WS",
                name          = "Total (完整回覆時間)",
                response_time = 180_000,
                response_length = 0,
                exception     = TimeoutError("STREAM_END not received within 180s"),
            )
