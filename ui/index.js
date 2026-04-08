/**
 * index.js — Vue 3 application entry point
 *
 * Imports Vue 3 from the unpkg CDN (no bundler required).
 * Wires together the auth composable and API service into a single-page app.
 */

import { createApp } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js';
import { token, buildGitHubAuthUrl, exchangeCode } from './composables/useAuth.js';
import { apiFetch } from './services/api.js';

createApp({
  data() {
    return {
      // auth
      isAuthenticated:      false,
      githubAuthUrl:        '#',
      authError:            '',
      // categories
      categories:           [],
      categoriesLoading:    false,
      categoriesError:      '',
      selectedCategoryCode: null,
      // messages
      messages:             [],
      messagesLoading:      false,
      messagesError:        '',
    };
  },

  computed: {
    selectedCategoryName() {
      const cat = this.categories.find(c => c.code === this.selectedCategoryCode);
      return cat ? cat.name : null;
    },
  },

  async created() {
    const params = new URLSearchParams(window.location.search);
    const code   = params.get('code');
    const state  = params.get('state');

    // ── Case 1: OAuth2 callback — exchange the code for a token ──
    if (code) {
      const savedState = sessionStorage.getItem('oauth_state');
      if (state && savedState && state !== savedState) {
        this.authError    = 'OAuth state mismatch — possible CSRF. Please try again.';
        this.githubAuthUrl = buildGitHubAuthUrl();
        token.clear();
        history.replaceState({}, '', window.location.pathname);
        return;
      }
      sessionStorage.removeItem('oauth_state');
      history.replaceState({}, '', window.location.pathname);

      try {
        const accessToken = await exchangeCode(code);
        token.save(accessToken);
      } catch (err) {
        this.authError    = `Sign-in failed: ${err.message}`;
        this.githubAuthUrl = buildGitHubAuthUrl();
        return;
      }
    }

    // ── Case 2: Token already present — show the app ─────────────
    if (token.get()) {
      this.isAuthenticated = true;
      await this.loadCategories();
      return;
    }

    // ── Case 3: Not authenticated — show the login screen ────────
    this.githubAuthUrl = buildGitHubAuthUrl();
  },

  methods: {
    /**
     * Fetch a path from api-rs with the stored Bearer token.
     * Redirects to the GitHub login page on 401.
     */
    async apiFetch(path) {
      try {
        return await apiFetch(path, token.get);
      } catch (err) {
        if (err.status === 401) {
          token.clear();
          window.location.href = buildGitHubAuthUrl();
        }
        throw err;
      }
    },

    /** Fetch all categories from api-rs and store them. */
    async loadCategories() {
      this.categoriesLoading = true;
      this.categoriesError   = '';
      try {
        this.categories = await this.apiFetch('/categories');
      } catch (err) {
        this.categoriesError = err.message;
      } finally {
        this.categoriesLoading = false;
      }
    },

    /** Select a category and load its messages. */
    async selectCategory(cat) {
      this.selectedCategoryCode = cat.code;
      this.messages             = [];
      this.messagesError        = '';
      this.messagesLoading      = true;
      try {
        this.messages = await this.apiFetch(
          `/messages?categoryCode=${encodeURIComponent(cat.code)}`
        );
      } catch (err) {
        this.messagesError = err.message;
      } finally {
        this.messagesLoading = false;
      }
    },
  },
}).mount('#app');
