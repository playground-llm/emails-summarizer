## ADDED Requirements

### Requirement: Display email list in sidebar
The sidebar SHALL display a scrollable list of emails. Each list item SHALL show at minimum: sender name, subject line (truncated if necessary), and received timestamp.

#### Scenario: Sidebar renders email items
- **WHEN** the page loads
- **THEN** the left sidebar SHALL display one entry per email in the data set, each showing sender, subject, and timestamp

#### Scenario: Long subject is truncated
- **WHEN** an email subject exceeds the available width of the sidebar item
- **THEN** the subject text SHALL be truncated with an ellipsis so the item fits on a single line

---

### Requirement: Select an email from the sidebar
The sidebar SHALL allow the user to select an email by clicking on its list item. Only one email SHALL be selected at a time.

#### Scenario: User clicks an email item
- **WHEN** the user clicks on an email list item
- **THEN** that item SHALL become the selected item and the selection SHALL be visually highlighted

#### Scenario: Previously selected item is deselected
- **WHEN** the user clicks a different email item while another is already selected
- **THEN** the previous item's highlight SHALL be removed and only the newly clicked item SHALL appear selected

#### Scenario: First email is pre-selected on load
- **WHEN** the page first loads and at least one email exists in the data set
- **THEN** the first email SHALL be automatically selected so the detail panel is never empty
