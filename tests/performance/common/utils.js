import http from 'k6/http';

/**
 * Shared utilities for k6 performance tests
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * Initialize a session and get client info
 * returns { sessionId: string }
 */
export function getInitialInfo() {
    const url = `${BASE_URL}/chat/initial`;
    const params = {
        headers: getHeaders(),
    };
    const res = http.post(url, null, params);
    
    if (res.status !== 200) {
        console.error(`Failed to initialize session: ${res.status} ${res.body}`);
        return null;
    }
    
    try {
        return res.json();
    } catch (e) {
        console.error(`Failed to parse initial info: ${e.message}`);
        return null;
    }
}

/**
 * Common headers for JSON API requests
 */
export function getHeaders() {
    return {
        'Content-Type': 'application/json',
    };
}

/**
 * Standard sleep durations (in seconds)
 */
export const SLEEP_DURATION = {
    MESSAGING: 2,
    POLLING: 1,
};
