/**
 * k6 Load Test: SSE Connection Stress
 *
 * Simulates 1000 concurrent users establishing EventSource (SSE) connections
 * ramping up over 60 seconds to validate the backend's connection handling.
 *
 * LIMITATION: k6's http.get() is NOT a true EventSource. It waits for the
 * entire HTTP response to complete (or timeout) before returning, rather than
 * processing chunks progressively. This means:
 *   - SSE connections will hit the timeout and report that as response time
 *   - Chunk-level timing metrics are not available
 *   - This script validates connection establishment and HTTP-level behavior,
 *     not real-time streaming throughput
 * For true SSE simulation, use the xk6-sse extension:
 *   https://github.com/phymbert/xk6-sse
 *
 * Usage:
 *   k6 run stress_sse.js
 *   k6 run --env BASE_URL=https://staging.example.com stress_sse.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const sseConnections = new Counter('sse_connections_established');
const sseErrors = new Counter('sse_connection_errors');
const sseFirstEventLatency = new Trend('sse_first_event_latency_ms');
const sseChunkCount = new Counter('sse_chunks_received');

export const options = {
  scenarios: {
    sse_spike: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 500 },   // ramp to 500 users in 30s
        { duration: '30s', target: 1000 },   // ramp to 1000 users in 30s
        { duration: '60s', target: 1000 },   // sustain 1000 users for 60s
        { duration: '30s', target: 0 },      // ramp down
      ],
    },
  },
  thresholds: {
    'sse_connection_errors': ['count<50'],            // <5% error rate at 1000 VUs
    'sse_first_event_latency_ms': ['p(95)<5000'],     // 95th percentile under 5s
    'http_req_failed': ['rate<0.05'],                 // HTTP failures under 5%
  },
};

export default function () {
  // 1. Create a session
  const initRes = http.post(`${BASE_URL}/chat/initial`, null, {
    headers: { 'Content-Type': 'application/json' },
  });

  const initOk = check(initRes, {
    'session created (200)': (r) => r.status === 200,
    'sessionId present': (r) => {
      try { return JSON.parse(r.body).sessionId !== undefined; } catch { return false; }
    },
  });

  if (!initOk) {
    sseErrors.add(1);
    return;
  }

  const sessionId = JSON.parse(initRes.body).sessionId;

  // 2. Open SSE connection (long-lived HTTP GET)
  const sseStart = Date.now();
  const sseRes = http.get(`${BASE_URL}/chat/${sessionId}/stream`, {
    timeout: '120s',
    headers: { Accept: 'text/event-stream' },
    tags: { name: 'SSE_STREAM' },
  });

  const sseOk = check(sseRes, {
    'SSE connected (200)': (r) => r.status === 200,
    'Content-Type is event-stream': (r) =>
      r.headers['Content-Type'] && r.headers['Content-Type'].includes('text/event-stream'),
  });

  if (sseOk) {
    sseConnections.add(1);
    const firstEventTime = Date.now() - sseStart;
    sseFirstEventLatency.add(firstEventTime);

    // Count SSE chunks in the response body
    if (sseRes.body) {
      const chunks = sseRes.body.split('event: chat').length - 1;
      sseChunkCount.add(chunks);
    }
  } else {
    sseErrors.add(1);
  }

  // 3. Send a test message to trigger LLM processing
  const sendRes = http.post(
    `${BASE_URL}/chat/${sessionId}/send`,
    JSON.stringify({ content: `k6 stress test message from VU ${__VU}` }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(sendRes, {
    'message sent (200)': (r) => r.status === 200,
  });

  // Hold the connection briefly to simulate real user behavior
  sleep(Math.random() * 5 + 2);
}
