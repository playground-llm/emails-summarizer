## 1. Project Setup

- [ ] 1.1 Add a comment in `ui/build.gradle` documenting Vue.js as the chosen frontend technology
- [ ] 1.2 Update `openspec/config.yaml` to record the Vue.js tech stack decision for the `ui` subproject

## 2. HTML Shell & Layout

- [ ] 2.1 Create `ui/index.html` with the HTML5 boilerplate (`<!DOCTYPE html>`, `<meta charset>`, `<meta viewport>`, `<title>`)
- [ ] 2.2 Add a `<style>` block implementing the full-viewport flex layout (sidebar ~320px fixed-width, detail panel flex-grow)
- [ ] 2.3 Style the sidebar to be independently scrollable and visually distinct (background color, border)
- [ ] 2.4 Style the detail panel to be independently scrollable with appropriate padding

## 3. Vue App Bootstrap

- [ ] 3.1 Add `<script type="module">` and import Vue 3 from the unpkg CDN (`vue.esm-browser.js`)
- [ ] 3.2 Define the Vue app with `createApp({})` and mount it to the root `<div id="app">`
- [ ] 3.3 Define mock email data (5–10 items) in the `data()` function — each with `id`, `sender`, `email`, `subject`, `date`, and `body` fields

## 4. Email List Sidebar

- [ ] 4.1 Render the email list in the sidebar using `v-for` over the emails array
- [ ] 4.2 Each list item displays sender name, truncated subject (CSS `text-overflow: ellipsis`), and formatted timestamp
- [ ] 4.3 Auto-select the first email on load by initialising `selectedId` to the first email's `id` in `data()`
- [ ] 4.4 Apply a visual highlight (e.g. distinct background colour) to the currently selected sidebar item using `:class` binding
- [ ] 4.5 Handle click on a sidebar item to update `selectedId` via a `selectEmail(id)` method

## 5. Email Detail Panel

- [ ] 5.1 Derive the `selectedEmail` object with a Vue `computed` property that finds the email matching `selectedId`
- [ ] 5.2 Render the detail panel showing `selectedEmail.subject`, `selectedEmail.sender`, `selectedEmail.email`, `selectedEmail.date`, and `selectedEmail.body` using `v-if`

## 6. Verification

- [ ] 6.1 Open `ui/index.html` directly in a browser and confirm the two-panel layout renders correctly
- [ ] 6.2 Confirm the first email is pre-selected and its details appear in the right panel on load
- [ ] 6.3 Click each sidebar item and verify the detail panel updates and the highlight moves correctly
- [ ] 6.4 Confirm a long email body scrolls the detail panel without affecting the sidebar
