## ADDED Requirements

### Requirement: Display selected email details
The detail panel SHALL display the full details of the currently selected email. It SHALL show: subject, sender name and email address, received date/time, and body text (or summary).

#### Scenario: Detail panel reflects selected email
- **WHEN** an email is selected in the sidebar
- **THEN** the right detail panel SHALL immediately update to show that email's subject, sender, date, and body

#### Scenario: Detail panel is never blank on load
- **WHEN** the page first loads
- **THEN** the detail panel SHALL show the details of the first email in the list (matching the auto-selected sidebar item)

---

### Requirement: Detail panel layout
The detail panel SHALL occupy the remaining horizontal space after the sidebar. It SHALL be scrollable independently of the sidebar when the body content overflows.

#### Scenario: Panel fills remaining space
- **WHEN** the page is rendered at any viewport width above the minimum
- **THEN** the detail panel SHALL fill all horizontal space to the right of the sidebar

#### Scenario: Long email body scrolls within the panel
- **WHEN** the selected email's body is longer than the visible area of the detail panel
- **THEN** the detail panel SHALL scroll vertically without affecting the sidebar scroll position
