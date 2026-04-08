/**
 * api.js — API service layer
 *
 * Provides the base URL and a fetch wrapper that attaches the Bearer token
 * from sessionStorage and handles 401 Unauthorized responses.
 */

/** Base URL of the api-rs backend. Override via window.__API_BASE__ if needed. */
export const API_BASE = window.__API_BASE__ || 'http://local.example.com:8080';

/**
 * Fetch a JSON resource from api-rs with an Authorization: Bearer header.
 *
 * @param {string} path        - Path relative to API_BASE (e.g. '/categories')
 * @param {() => string} getToken - Function that returns the current access token
 * @returns {Promise<any>}     - Parsed JSON response body
 * @throws {Error}             - On HTTP error; status 401 includes { status: 401 }
 */
export async function apiFetch(path, getToken) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${getToken()}` },
  });
  if (res.status === 401) {
    const err = new Error('Unauthorized');
    err.status = 401;
    throw err;
  }
  if (!res.ok) throw new Error(`API error ${res.status} on ${path}`);
  return res.json();
}
