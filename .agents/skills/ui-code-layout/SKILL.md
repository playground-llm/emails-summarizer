---
name: ui-code-layout
description: Establised the layout of the UI code, including the structure of components, folders, and files.
compatibility: All UI codebases, inside ui folder.
---

# UI Code Layout Skill

Understand and establish the layout of the UI code, including the structure of components, folders, and files.
This should be done before writing any UI code, to ensure that the codebase is organized and maintainable.
The ui is placed under the `ui` folder, and the structure should be consistent across the codebase.
The ui project layout should be as follow:

```
ui/
  ├── styles/       ← Global styles (CSS, SCSS, etc.)
  ├── assets/       ← Images, fonts, and other static assets
  ├── utils/        ← Utility functions and helpers
  ├── scripts/      ← Build scripts, configuration files, etc.
  ├── components/   ← Reusable UI components (buttons, inputs, etc.)
  ├── views/        ← Page-level components (used with Vue Router)
  ├── router/       ← Vue Router config (index.js)
  ├── stores/       ← Pinia state management
  ├── composables/  ← Reusable composition functions (useXxx.js)
  ├── services/     ← API calls / business logic
  ├── types/        ← TypeScript type definitions (if using TS)
  ├── index.js      ← Main entry point for the UI code
  └── index.html    ← Main HTML file (if applicable)
```

For ui should be used only js, not typescript, so the `types` folder is not necessary.

## Usage

All the code under 'ui' folder should follow the above structure, and the components should be organized in a way that makes sense for the project.
The content discovered with 'ui' should be placed in the appropriate folders, and the code should be written in a way that is consistent with the overall structure of the codebase.
This will help to ensure that the code is maintainable and easy to understand for other developers who may work on the project in the future.

## Steps

1. Check the existing structure of the 'ui' folder and identify any inconsistencies or areas for improvement.
2. Organize the components, views, and other files according to the established structure.
3. Ensure that all new code follows the defined layout and naming conventions.
4. Regularly review the codebase to maintain the organization and make adjustments as needed.
5. Document the structure and guidelines for the UI code to help onboard new developers and maintain consistency across the team.
