## ADDED Requirements

### Requirement: Fetch and display categories in sidebar
The sidebar SHALL fetch all categories from `GET /categories` (api-rs) on page load using a valid Bearer token, and render them as a scrollable list. Each list item SHALL display the category `name`.

#### Scenario: Categories are loaded on page mount
- **WHEN** the page loads and the user holds a valid access token
- **THEN** the sidebar SHALL display one item per category returned by `GET /categories`, showing the category name

#### Scenario: Empty category list
- **WHEN** `GET /categories` returns an empty array
- **THEN** the sidebar SHALL display an empty state (no items; optionally a "No categories" message)

#### Scenario: API error during category fetch
- **WHEN** `GET /categories` returns a non-2xx response
- **THEN** the sidebar SHALL display an error indicator and SHALL NOT crash the page

---

### Requirement: Select a category from the sidebar
The sidebar SHALL allow the user to select a category by clicking its list item. Only one category SHALL be selected at a time. Selecting a category SHALL trigger a messages fetch for that category's `code`.

#### Scenario: User clicks a category item
- **WHEN** the user clicks on a category list item
- **THEN** that item SHALL become selected, visually highlighted, and the right panel SHALL load messages for that category's code

#### Scenario: First category is auto-selected on load
- **WHEN** the page loads and at least one category is returned by the API
- **THEN** the first category SHALL be automatically selected so the message panel is never empty on initial render

#### Scenario: Previously selected category loses highlight on new selection
- **WHEN** the user clicks a different category while another is already selected
- **THEN** only the newly clicked category SHALL be highlighted
