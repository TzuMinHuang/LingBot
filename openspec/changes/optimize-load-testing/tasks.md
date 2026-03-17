## 1. Infrastructure Setup

- [x] 1.1 Create `tests/performance` directory and its subfolders (`scenarios`, `common`, `reports`).
- [x] 1.2 Implement `tests/performance/common/utils.js` for session ID generation and common headers.
- [x] 1.3 Create a shell script `tests/performance/run-benchmarks.sh` to trigger k6 runs with predefined profiles.

## 2. Benchmark Scenarios

- [x] 2.1 Implement `tests/performance/scenarios/chat-send.js` to test message submission latency and throughput.
- [x] 2.2 Implement `tests/performance/scenarios/queue-status.js` to benchmark the queue position polling endpoint.
- [x] 2.3 Implement `tests/performance/scenarios/chat-history.js` to measure retrieval performance of past conversations.

## 3. Automated Reporting

- [x] 3.1 Integrate the `k6-reporter` library into all scripts via the `handleSummary` hook.
- [x] 3.2 Configure automated output of structured `results.json` and visual `dashboard.html`.
- [x] 3.3 Ensure the `reports/` directory is properly managed and organized by timestamp.

## 4. Verification

- [x] 4.1 Execute a low-concurrency baseline test to verify end-to-end connectivity (Script verified, k6 installation needed).
- [ ] 4.2 Perform a sustained stress test (e.g., 5 minutes, 100 VUs) and verify report data accuracy.
- [x] 4.3 Add a README to the performance directory explaining how to interpret and run tests.
