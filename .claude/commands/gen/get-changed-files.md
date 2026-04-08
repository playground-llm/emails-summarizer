---
name: "GEN: Get changed files"
description: Show all changed files in the current git repository, grouped by staged, unstaged, and untracked
category: Workflow
tags: [workflow, git]
---

Get changed files

Show all changed files in the current git repository, grouped by status.

Run the following and report results:

1. Get short status: !git status --short
2. Get diff summary against HEAD: !git diff --stat HEAD

Present results in three groups:

- **Staged** — files with index changes (lines starting with a letter in column 1)
- **Unstaged** — files with working tree changes (lines starting with a letter in column 2)
- **Untracked** — new files not yet added (lines starting with `??`)

For each file include its path (as a markdown link relative to repo root) and the type of change (modified, added, deleted, renamed).
