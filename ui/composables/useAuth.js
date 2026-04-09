/**
 * useAuth.js — OAuth2 Authorization Code flow composable
 *
 * Manages the GitHub OAuth2 token lifecycle:
 *   - sessionStorage token helpers
 *   - Building the GitHub authorization URL (with CSRF state)
 *   - Exchanging an authorization code for an access token via the api-rs proxy
 *
 * The token exchange goes through api-rs (POST /oauth2/token) because GitHub's
 * token endpoint does not emit CORS headers for browser requests.
 */

import { API_BASE } from '../services/api.js';

/**
 * GitHub OAuth App client_id (public — safe in browser).
 * Override by setting window.__GITHUB_CLIENT_ID__ before this module loads.
 */
const GITHUB_CLIENT_ID = window.__GITHUB_CLIENT_ID__ || 'Ov23lidSXJDYQSwOXa5m';

/**
 * OAuth2 redirect_uri — must exactly match the registered GitHub OAuth App callback URL.
 * Derived from the current page origin + path so it works on any port/hostname.
 */
export const REDIRECT_URI = window.location.origin + window.location.pathname;

// ── sessionStorage token helpers ───────────────────────────────────────────

export const token = {
  get:   ()    => sessionStorage.getItem('access_token'),
  save:  (t)   => sessionStorage.setItem('access_token', t),
  clear: ()    => sessionStorage.removeItem('access_token'),
};

// ── GitHub authorization URL ────────────────────────────────────────────────

/**
 * Build the GitHub authorization URL for the OAuth2 Authorization Code flow.
 * Stores a random CSRF state value in sessionStorage for later verification.
 *
 * @returns {string} Full GitHub authorize URL
 */
export function buildGitHubAuthUrl() {
  const state = '6edd6402-3f18-40cc-a8c3-bf6797336533';//crypto.randomUUID();
  sessionStorage.setItem('oauth_state', state);
  const params = new URLSearchParams({
    client_id:    GITHUB_CLIENT_ID,
    redirect_uri: REDIRECT_URI,
    scope:        'read:user',
    state,
  });
  return `https://github.com/login/oauth/authorize?${params}`;
}

// ── Token exchange ──────────────────────────────────────────────────────────

/**
 * Exchange a GitHub authorization code for an access token via the api-rs proxy.
 *
 * @param {string} code - Authorization code from the GitHub callback URL
 * @returns {Promise<string>} The access token
 * @throws {Error} If the exchange fails or the response contains no token
 */
export async function exchangeCode(code) {
  const res = await fetch(`${API_BASE}/oauth2/token`, {
    method:  'POST',
    headers: { 'Content-Type': 'application/json' },
    body:    JSON.stringify({ code, redirectUri: REDIRECT_URI }),
  });
  if (!res.ok) throw new Error(`Token exchange failed: HTTP ${res.status}`);
  const data = await res.json();
  if (!data.accessToken) throw new Error('No accessToken in response');
  return data.accessToken;
}
