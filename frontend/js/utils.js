/**
 * fetch with AbortController timeout.
 * @param {string} url
 * @param {RequestInit} options
 * @param {number} timeoutMs - default 8 seconds
 * @returns {Promise<Response>}
 */
export function fetchWithTimeout(url, options = {}, timeoutMs = 8000) {
  const controller = new AbortController();
  const id = setTimeout(() => controller.abort(), timeoutMs);
  return fetch(url, { ...options, signal: controller.signal })
    .finally(() => clearTimeout(id));
}
