import asyncio
import aiohttp
import time
import sys
from collections import Counter

# 測試配置
BASE_HOST = "http://localhost"  # 請根據實際 Nginx 或 Backend 位址修改
INIT_ENDPOINT = "/bot/chat/initial"
SEND_PATH_TEMPLATE = "/bot/chat/{}/send"
CONCURRENT_REQUESTS = 5
EXPECTED_SUCCESS = 1

async def init_session(session):
    """初始化並取得 sessionId"""
    url = f"{BASE_HOST}{INIT_ENDPOINT}"
    async with session.post(url) as response:
        if response.status == 200:
            data = await response.json()
            return data.get("sessionId")
    return None

async def send_request(session, request_id, session_id, results):
    """發送單個 POST 請求並記錄結果"""
    url = f"{BASE_HOST}{SEND_PATH_TEMPLATE.format(session_id)}"
    payload = {
        "content": f"Idempotency test request {request_id}"
    }
    
    try:
        start_time = time.time()
        async with session.post(url, json=payload, timeout=10) as response:
            status = response.status
            elapsed = (time.time() - start_time) * 1000
            results.append(status)
            print(f"Request {request_id}: Status {status}, Latency: {elapsed:.2f}ms")
    except Exception as e:
        print(f"Request {request_id}: Failed with error: {e}")
        results.append("ERROR")

async def run_idempotency_test():
    """執行併發測試邏輯"""
    print(f"=== Redis Idempotency Guard Test ===")
    print(f"Target Host: {BASE_HOST}")
    
    async with aiohttp.ClientSession() as session:
        # 1. 取得有效的 sessionId
        session_id = await init_session(session)
        if not session_id:
            print("❌ Error: Failed to initialize session.")
            return
            
        print(f"Test Session ID: {session_id}")
        print(f"Logic: Sending {CONCURRENT_REQUESTS} requests with the SAME sessionId simultaneously.\n")

        # 2. 併發猛攻
        results = []
        tasks = [
            send_request(session, i + 1, session_id, results) 
            for i in range(CONCURRENT_REQUESTS)
        ]
        await asyncio.gather(*tasks)

    # 3. 統計結果
    print(f"\n=== Test Statistics ===")
    stats = Counter(results)
    for status, count in stats.items():
        print(f"Status {status}: {count} requests")
    
    success_count = stats.get(200, 0)
    
    print(f"\n=== Validation ===")
    if success_count == EXPECTED_SUCCESS:
        print("✅ PASS: Exactly one request was accepted (200 OK).")
    elif success_count > EXPECTED_SUCCESS:
        print(f"❌ FAIL: Idempotency failed! {success_count} requests were accepted (Expected {EXPECTED_SUCCESS}).")
    else:
        print(f"⚠️ WARNING: No requests succeeded (Expected {EXPECTED_SUCCESS}). Check status codes above.")

if __name__ == "__main__":
    if len(sys.argv) > 1:
        BASE_HOST = sys.argv[1].rstrip("/")
        
    asyncio.run(run_idempotency_test())
