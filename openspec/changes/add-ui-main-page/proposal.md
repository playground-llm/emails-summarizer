## Why

The `ui` subproject currently has no frontend implementation — it is an empty Gradle stub. Users need a working main page to interact with the emails-summarizer application, so we are establishing the Vue.js foundation and the primary two-panel layout that all subsequent UI work will build upon.

## What Changes

- Add `ui/index.html` as the application entry point with a Vue 3 (CDN) setup.
- Implement a two-panel layout: a scrollable email-list sidebar on the left and an email-detail panel on the right.
- Left sidebar lists email items (sender, subject preview, timestamp); clicking an item selects it.
- Right panel displays the full details of the selected email (subject, sender, date, body summary).
- Wire up basic sample/mock data so the page works standalone without a backend.
- Update `ui/build.gradle` to reflect the Vue.js frontend technology choice.

## Capabilities

### New Capabilities

- `email-list-sidebar`: Display a scrollable list of emails in the left sidebar; supports item selection.
- `email-detail-panel`: Display the full details of a selected email in the right content panel.

### Modified Capabilities

*(none — no existing specs)*

## Impact

- **Files added**: `ui/index.html`
- **Files modified**: `ui/build.gradle` (technology annotation/comment)
- **Dependencies introduced**: Vue 3 via CDN (no build tooling required at this stage)
- **No API contract changes** — mock data is self-contained in the page
- **No breaking changes**
