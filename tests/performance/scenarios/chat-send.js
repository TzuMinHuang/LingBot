import http from 'k6/http';
import { sleep, check } from 'k6';
import { jUnit, textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { getInitialInfo, getHeaders, BASE_URL } from '../common/utils.js';

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.01'], // <1% errors
        http_req_duration: ['p(95)<500'], // 95% of requests < 500ms
    },
};

export default function () {
    const initialInfo = getInitialInfo();
    if (!initialInfo || !initialInfo.sessionId) {
        return;
    }

    const sessionId = initialInfo.sessionId;
    const url = `${BASE_URL}/chat/${sessionId}/send`;
    const payload = JSON.stringify({
        content: 'This is a performance test message.',
    });

    const params = {
        headers: getHeaders(),
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has interactionId': (r) => r.json().interactionId !== undefined,
    });

    // Simulate human interaction time: 1-3 seconds
    sleep(Math.random() * 2 + 1);
}

import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const reportPath = `tests/performance/reports/chat-send_${timestamp}.html`;
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [reportPath]: htmlReport(data),
        'tests/performance/reports/results.json': JSON.stringify(data),
    };
}
