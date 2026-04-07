## Context

The `ui` subproject is a bare Gradle stub with no frontend code. The emails-summarizer application needs a starting point that allows users to browse a list of emails and view their details. The layout follows a standard master-detail pattern (email client style), with the email list on the left and the selected email's content on the right — as referenced in the design mockup.

Since the tech stack is not yet locked in and no build pipeline exists for the `ui` subproject, the implementation must be self-contained in a single HTML file using Vue 3 via CDN, with no bundler or build step required.

## Goals / Non-Goals

**Goals:**
- Deliver a functional `ui/index.html` that renders without a build tool (Vue 3 CDN).
- Two-panel layout: left sidebar = email list, right panel = email detail.
- Clicking an email in the sidebar selects it and updates the detail panel.
- Responsive visual treatment matching the master-detail email client style.
- Sample mock data (5–10 emails) baked into the page so it works standalone.
- Document the Vue.js technology choice in `ui/build.gradle`.

**Non-Goals:**
- Real API integration or HTTP fetching.
- User authentication or sessions.
- Email compose, reply, or send functionality.
- Build tooling (Vite, webpack, etc.) — that is a future concern.
- Responsive/mobile breakpoints beyond basic desktop layout.

## Decisions

### Decision: Vue 3 via CDN (no bundler)

**Choice**: Load Vue 3 from `https://unpkg.com/vue@3/dist/vue.esm-browser.js` using `<script type="module">`.

**Rationale**: No Gradle plugin or JS toolchain is established yet. A single-file CDN approach unblocks frontend development immediately without requiring a build pipeline. This is explicitly a scaffolding/bootstrapping step; the build system can be added in a follow-up change.

**Alternative considered**: Vite — adds value but requires Node.js toolchain and Gradle plugin wiring, which is scope-creep for a first-pass page.

---

### Decision: Single HTML file with inline Vue component

**Choice**: One `ui/index.html` file with Vue app defined inside a `<script type="module">` block. CSS in a `<style>` block in the same file.

**Rationale**: Keeps the initial scaffolding self-contained and easy to open directly in a browser (`file://` or any local server). Component splitting can be introduced once a bundler is added.

**Alternative considered**: Separate `.js` and `.css` files — slightly cleaner but adds complexity without benefit at this stage.

---

### Decision: Mock data inline in the Vue `data()` function

**Choice**: An array of plain JavaScript objects representing emails, defined directly in the Vue component's `data()` function.

**Rationale**: The page must work without a backend. Keeping mock data in the same file makes the proof-of-concept immediately runnable. When the API is ready, the array is simply replaced with a fetch call.

---

### Decision: Layout using CSS Flexbox

**Choice**: A full-viewport flex container: fixed-width left sidebar (~320 px) + flex-grow right panel.

**Rationale**: Straightforward to implement, widely supported, and matches the master-detail mockup.

## Risks / Trade-offs

- **CDN dependency at runtime** → If `unpkg.com` is unavailable the page won't load. Mitigation: download `vue.esm-browser.js` locally once the build pipeline is added.
- **No build step means no tree-shaking or minification** → Acceptable for scaffolding; address when Vite/webpack is introduced.
- **Mock data is not persistent** → Selections and state reset on page refresh. Expected for this phase.
- **`ui/build.gradle` is still effectively a no-op** → Adding a comment noting Vue.js is the chosen technology is sufficient until a Gradle JS plugin is selected.
