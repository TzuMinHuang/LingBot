/**
 * Environment configuration — single source of truth.
 *
 * All settings are loaded from /config/env.json.
 * Both env-config.js (ES module) and chatbot-widget.js (IIFE) read from the same file.
 *
 * Detection order:
 *   1. Explicit envOverride parameter
 *   2. Hostname-based auto-detection (hostEnvMap)
 *   3. Fallback to 'dev'
 */

let _configCache = null;

async function loadConfig() {
  if (_configCache) return _configCache;
  const res = await fetch('/config/env.json');
  _configCache = await res.json();
  return _configCache;
}

function detectEnv(hostEnvMap) {
  return hostEnvMap[window.location.hostname] || 'dev';
}

export async function getEnvConfig(envOverride) {
  const config = await loadConfig();
  const env = envOverride || detectEnv(config.hostEnvMap);
  const envSettings = config.envMap[env];
  if (!envSettings) {
    console.warn(`[env-config] Unknown env "${env}", falling back to dev`);
    return { ...config.envMap.dev, env: 'dev', ui: config.ui || {} };
  }
  return { ...envSettings, env, ui: config.ui || {} };
}
