## ADDED Requirements

### Requirement: Fetch and display messages for selected category
The right panel SHALL fetch messages from `GET /messages?categoryCode={code}` (api-rs) whenever a category is selected, using a valid Bearer token. Each message item SHALL display the `title` and a truncated preview of `body`.

#### Scenario: Messages are loaded when a category is selected
- **WHEN** the user selects a category in the sidebar
- **THEN** the right panel SHALL fetch `GET /messages?categoryCode={code}` and display the returned messages, each showing `title` and a body preview

#### Scenario: No messages for selected category
- **WHEN** `GET /messages?categoryCode={code}` returns an empty array
- **THEN** the right panel SHALL display an empty state message (e.g. "No messages in this category")

#### Scenario: API error during message fetch
- **WHEN** `GET /messages?categoryCode={code}` returns a non-2xx response
- **THEN** the right panel SHALL display an error indicator and SHALL NOT crash the page

---

### Requirement: Message panel layout and scrolling
The right panel SHALL occupy all horizontal space not taken by the sidebar. It SHALL scroll vertically and independently of the sidebar when the message list overflows.

#### Scenario: Panel fills remaining horizontal space
- **WHEN** the page is rendered
- **THEN** the message panel SHALL fill all viewport width to the right of the sidebar

#### Scenario: Long message list scrolls within the panel
- **WHEN** the number of messages exceeds the visible height of the panel
- **THEN** the panel SHALL scroll vertically without affecting the sidebar scroll position
