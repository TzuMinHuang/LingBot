# Performance Testing Suite

This directory contains an automated benchmarking framework using [k6](https://k6.io/).

## Structure

- `/scenarios`: Benchmark scripts for specific endpoints.
- `/common`: Shared utilities (session ID generation, headers).
- `/reports`: Automatically generated HTML dashboards and JSON metrics.

## Prerequisites

You must have `k6` installed:
- **macOS**: `brew install k6`
- **Docker**: `docker pull grafana/k6`
- **Other**: See [k6 docs](https://k6.io/docs/getting-started/installation/)

## Running Benchmarks

Use the provided runner script:

```bash
./tests/performance/run-benchmarks.sh <target_url> <scenario> <profile>
```

### Examples:

1. **Baseline test (1 VU, 30s) against Chat Sending:**
   ```bash
   ./tests/performance/run-benchmarks.sh http://localhost:8080 chat-send baseline
   ```

2. **Load test (10 VUs, 2m) against Queue Position:**
   ```bash
   ./tests/performance/run-benchmarks.sh http://localhost:8080 queue-status load
   ```

3. **Stress test (50 VUs, 5m) against Chat History:**
   ```bash
   ./tests/performance/run-benchmarks.sh http://localhost:8080 chat-history stress
   ```

## Reports

After each run, a visual HTML report and a structured `results.json` will be generated in `tests/performance/reports/`.
