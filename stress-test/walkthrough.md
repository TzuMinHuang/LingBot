# Walkthrough - AI Chat SSE Performance Testing

I have developed a specialized Python Locust test script designed to simulate concurrent users and measure the performance of your SSE-based AI chat system.

## Changes Made

### Performance Testing
- **New Script**: Created [locustfile_sse.py](file:///Users/j/web/chatbot/LingBot/stress-test/locustfile_sse.py) which supports:
    - **SSE Streaming**: Correct pipeline for handling `text/event-stream` using `requests` with `stream=True`.
    - **Custom Metrics**: 
        - **Queueing Latency (TTFT)**: Measures the time from request start to the first token received.
        - **TPS (Tokens Per Second)**: Measures the generation throughput.
    - **Error Handling**: Captures and reports `429 Too Many Requests` status codes and connection timeouts.
    - **Configurability**: Easily switch endpoints by passing the `--host` argument to Locust.

## Verification Results

### Automated Tests
- **Syntax Check**: Verified that the script compiles correctly without Python syntax errors.
- **Metric Logic**: The script includes explicit events for reporting `Queueing Latency (TTFT)` and `Token Generation Speed (TPS)`, which will appear in the Locust web interface.

## How to use

1. **Setup Environment**:
   ```bash
   pip install locust
   ```

2. **Run Stress Test**:
   To test a specific backend (e.g., vLLM):
   ```bash
   locust -f stress-test/locustfile_sse.py --host http://vllm-endpoint:8000
   ```

3. **Scale Users**:
   Use the Locust Web UI (usually at http://localhost:8089) to set:
   - **Number of users**: 10 progressing to 100.
   - **Spawn rate**: 10 users/second.

4. **Monitor Metrics**:
   - Check the **Statistics** tab for Response Time.
   - Look for custom names like `Queueing Latency (TTFT)` and `Token Generation Speed (TPS)` in the transaction list.
   - Monitor the **Failures** tab for 429 errors.
