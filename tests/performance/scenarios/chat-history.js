import http from 'k6/http';
import { sleep, check } from 'k6';
import { jUnit, textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { generateSessionId, getHeaders, BASE_URL } from '../common/utils.js';

export const options = {
    thresholds: {
        http_req_failed: ['rate<0.01'], 
        http_req_duration: ['p(95)<300'], 
    },
};

export default function () {
    const initialInfo = getInitialInfo();
    if (!initialInfo || !initialInfo.sessionId) {
        return;
    }

    const sessionId = initialInfo.sessionId;
    const url = `${BASE_URL}/chat/${sessionId}/history`;

    const params = {
        headers: getHeaders(),
    };

    const res = http.get(url, params);

    check(res, {
        'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
    });

    sleep(2);
}

import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    const reportPath = `tests/performance/reports/chat-history_${timestamp}.html`;
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
        [reportPath]: htmlReport(data),
        'tests/performance/reports/results.json': JSON.stringify(data),
    };
}
