## Context

The chatbot backend is built on a reactive architecture (Spring WebFlux, Redis Streams) designed for high concurrency. However, without automated stress testing, we cannot verify if the current implementation scales correctly or identifies bottleneck points in the asynchronous pipeline. This design introduces a lightweight, developer-friendly benchmarking suite.

## Goals / Non-Goals

**Goals:**
- Automate the execution of load tests against core endpoints.
- Provide clear visual (HTML) and structured (JSON) reporting.
- Enable testing of concurrent request handling and queue responsiveness.
- Allow developers to run benchmarks locally with minimal setup.

**Non-Goals:**
- Automating performance regression in CI (this phase is focus on infrastructure and local triggers).
- Designing a load balancer or auto-scaling logic based on these metrics.
- Simulation of browser-side frontend performance (LLM response rendering).

## Decisions

- **Engine: [k6](https://k6.io/)**: Selected for its modern JavaScript scripting, low resource footprint (written in Go), and excellent integration with standard metric formats.
- **Reporting: `k6-reporter`**: Use the `https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js` handle to transform end-of-test metrics into a standalone HTML dashboard.
- **Organization**: All performance related code will live in `tests/performance/`, with a `bin/run-benchmarks.sh` entry point.
- **Data Isolation**: Tests will generate session IDs with a `perf_test_` prefix to facilitate easy cleanup and monitoring identification in Redis.

## Risks / Trade-offs

- **[Risk] Resource Contention** → If tests run on the same machine as the backend/Redis, results may be skewed by context switching. *Mitigation*: Recommend running k6 from a separate terminal/process or a dedicated test container.
- **[Risk] Data Pollution** → High-volume testing will create many entries in Redis. *Mitigation*: Implement a `cleanup.js` script or use a short TTL for performance test sessions.
- **[Trade-off] Local vs. Distributed** → Local k6 execution is limited by the host's NIC and CPU. For massive scale, k6 cloud or distributed k6 would be needed, but local execution is sufficient for identifying internal backend bottlenecks.
