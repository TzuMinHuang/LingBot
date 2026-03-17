import http from 'k6/http';
import { sleep, check } from 'k6';
import { jUnit, textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { generateSessionId, getHeaders, BASE_URL } from '../common/utils.js';

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.01'], // <1% errors
        http_req_duration: ['p(99)<200'], // 99% of requests < 200ms
    },
};

export default function () {
    const sessionId = generateSessionId();
    const url = `${BASE_URL}/chat/${sessionId}/queue-position`;

    const params = {
        headers: getHeaders(),
    };

    const res = http.get(url, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
        'has position': (r) => r.json().position !== undefined,
    });

    sleep(0.5); // Fast polling simulation
}

import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const reportPath = `tests/performance/reports/queue-status_${timestamp}.html`;
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [reportPath]: htmlReport(data),
        'tests/performance/reports/results.json': JSON.stringify(data),
    };
}
