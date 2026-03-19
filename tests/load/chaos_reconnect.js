/**
 * k6 Chaos Test: SSE Reconnection with Last-Event-ID
 *
 * Simulates unpredictable user network drops during active LLM streaming
 * and validates that instant reconnections with Last-Event-ID correctly
 * resume the stream without data loss.
 *
 * LIMITATION: k6's http.get() is NOT a true EventSource. It waits for the
 * entire HTTP response to complete (or timeout) before returning, rather than
 * processing chunks progressively. The reconnect logic still validates the
 * HTTP layer and Last-Event-ID header handling correctly.
 * For true SSE simulation, use the xk6-sse extension:
 *   https://github.com/phymbert/xk6-sse
 *
 * Usage:
 *   k6 run chaos_reconnect.js
 *   k6 run --env BASE_URL=https://staging.example.com chaos_reconnect.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Custom metrics
const reconnectAttempts = new Counter('reconnect_attempts');
const reconnectSuccess = new Rate('reconnect_success_rate');
const reconnectLatency = new Trend('reconnect_latency_ms');
const dataLossEvents = new Counter('data_loss_events');

export const options = {
  scenarios: {
    chaos_reconnect: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 50 },    // ramp to 50 users
        { duration: '60s', target: 200 },    // ramp to 200 users with reconnection chaos
        { duration: '30s', target: 200 },    // sustain
        { duration: '15s', target: 0 },      // ramp down
      ],
    },
  },
  thresholds: {
    'reconnect_success_rate': ['rate>0.90'],       // 90%+ reconnects must succeed
    'reconnect_latency_ms': ['p(95)<3000'],        // reconnect within 3s at p95
    'data_loss_events': ['count<10'],              // near-zero data loss
    'http_req_failed': ['rate<0.10'],              // allow slightly higher failure for chaos
  },
};

/**
 * Extract the last event ID from an SSE response body.
 * Parses "id: <value>" lines and returns the last one found.
 */
function extractLastEventId(body) {
  if (!body) return null;
  const matches = body.match(/^id:\s*(.+)$/gm);
  if (!matches || matches.length === 0) return null;
  return matches[matches.length - 1].replace(/^id:\s*/, '').trim();
}

/**
 * Count the number of STREAM_CHUNK events in the SSE body.
 */
function countChunks(body) {
  if (!body) return 0;
  return (body.match(/"type"\s*:\s*"STREAM_CHUNK"/g) || []).length;
}

export default function () {
  // 1. Create session
  const initRes = http.post(`${BASE_URL}/chat/initial`, null, {
    headers: { 'Content-Type': 'application/json' },
  });

  if (initRes.status !== 200) return;
  const sessionId = JSON.parse(initRes.body).sessionId;

  // 2. Send a message to trigger LLM streaming
  const sendRes = http.post(
    `${BASE_URL}/chat/${sessionId}/send`,
    JSON.stringify({ content: `chaos test message VU-${__VU} iter-${__ITER}` }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (sendRes.status !== 200) return;

  // 3. Open initial SSE connection
  const sseRes1 = http.get(`${BASE_URL}/chat/${sessionId}/stream`, {
    timeout: '10s',
    headers: { Accept: 'text/event-stream' },
    tags: { name: 'SSE_INITIAL' },
  });

  const initialChunks = countChunks(sseRes1.body);
  const lastEventId = extractLastEventId(sseRes1.body);

  // 4. Simulate random network drop (abrupt disconnect after 1-3s)
  sleep(Math.random() * 2 + 1);

  // 5. Reconnect with Last-Event-ID (simulating browser EventSource reconnect)
  reconnectAttempts.add(1);
  const reconnectStart = Date.now();

  const reconnectHeaders = {
    Accept: 'text/event-stream',
  };
  if (lastEventId) {
    reconnectHeaders['Last-Event-ID'] = lastEventId;
  }

  const sseRes2 = http.get(`${BASE_URL}/chat/${sessionId}/stream`, {
    timeout: '15s',
    headers: reconnectHeaders,
    tags: { name: 'SSE_RECONNECT' },
  });

  const reconnectTime = Date.now() - reconnectStart;
  reconnectLatency.add(reconnectTime);

  const reconnected = check(sseRes2, {
    'reconnect succeeded (200)': (r) => r.status === 200,
    'reconnect returns event-stream': (r) =>
      r.headers['Content-Type'] && r.headers['Content-Type'].includes('text/event-stream'),
  });

  reconnectSuccess.add(reconnected ? 1 : 0);

  if (reconnected) {
    const reconnectChunks = countChunks(sseRes2.body);

    // If we had a lastEventId and got zero recovery chunks,
    // flag potential data loss (heuristic — not guaranteed to always have data)
    if (lastEventId && reconnectChunks === 0 && initialChunks > 0) {
      dataLossEvents.add(1);
    }
  }

  // 6. Random delay before next iteration to avoid synchronized thundering herd
  sleep(Math.random() * 3 + 1);
}
