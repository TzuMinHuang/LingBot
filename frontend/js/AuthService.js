import { fetchWithTimeout } from './utils.js';

/**
 * AuthService — Token management + SSO login via middleware API.
 *
 * Flow:
 *   1. Page load → checkSSOCallback() picks up token from URL if redirected back
 *   2. validateToken() → calls middleware API to check token validity
 *   3. Token valid → reconnect directly
 *   4. Token invalid/missing → redirectToSSO() → middleware handles OAuth2/SAML
 *   5. After SSO success → middleware redirects back with token in URL params
 *
 * Configuration (pass to constructor):
 *   - validateUrl : middleware endpoint to validate token
 *   - ssoUrl      : middleware endpoint that initiates OAuth2/SAML SSO
 */
export class AuthService {
  constructor({
    validateUrl = '/api/auth/validate',
    ssoUrl      = '/api/auth/sso'
  } = {}) {
    this._validateUrl = validateUrl;
    this._ssoUrl = ssoUrl;
    this._tokenKey = 'chatbot_token';

    // On load, check if we were redirected back from SSO with a token
    this._checkSSOCallback();
  }

  // ── Token storage ────────────────────────────────────────

  getToken() {
    return sessionStorage.getItem(this._tokenKey);
  }

  setToken(token) {
    sessionStorage.setItem(this._tokenKey, token);
  }

  clearToken() {
    sessionStorage.removeItem(this._tokenKey);
  }

  // ── Token validation ─────────────────────────────────────

  /**
   * Call middleware API to check if token is still valid.
   * Expected response: 200 OK = valid, 401 = expired/invalid.
   *
   * TODO: adjust endpoint/headers to match your middleware API spec.
   */
  async validateToken() {
    const token = this.getToken();
    if (!token) return false;

    try {
      const res = await fetchWithTimeout(this._validateUrl, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) return true;
      // Token expired — clear it
      this.clearToken();
      return false;
    } catch {
      return false;
    }
  }

  // ── SSO redirect ─────────────────────────────────────────

  /**
   * Redirect to middleware SSO endpoint.
   * The middleware will handle OAuth2/SAML negotiation with AD,
   * then redirect back to this page with token in URL.
   *
   * Expected redirect back: /chatbot.html?token=xxx
   *
   * TODO: adjust ssoUrl and returnUrl param name to match middleware spec.
   */
  redirectToSSO() {
    const returnUrl = encodeURIComponent(window.location.origin + window.location.pathname);
    window.location.href = `${this._ssoUrl}?returnUrl=${returnUrl}`;
  }

  // ── SSO callback handling ────────────────────────────────

  /**
   * Check URL params for token returned by middleware after SSO.
   * If found, store it and clean up the URL.
   *
   * TODO: adjust param name ('token') to match what your middleware returns.
   */
  _checkSSOCallback() {
    const params = new URLSearchParams(window.location.search);
    const token = params.get('token');
    if (token) {
      this.setToken(token);
      // Clean token from URL to avoid leaking / re-processing
      params.delete('token');
      const clean = params.toString();
      const newUrl = window.location.pathname + (clean ? `?${clean}` : '');
      window.history.replaceState({}, '', newUrl);
    }
  }
}
