## Context

The repository currently has a minimal `.gitignore` that fails to address multiple languages (Java, Python, JS) and environmental metadata (.metadata, .agent, .docker). This results in a "dirty" `git status` output and the inclusion of binary artifacts in the version control system.

## Goals / Non-Goals

**Goals:**
- Implement a comprehensive `.gitignore` covering Java/Maven, Python, Node.js, IDEs, and OS-specific files.
- Untrack all previously tracked files that match the new ignore rules.
- Ensure sensitive files (like `.env`) are strictly excluded.

**Non-Goals:**
- Deleting local files (only removing them from Git tracking).
- Reconfiguring Git global ignores.

## Decisions

- **Single Global .gitignore**: Maintain one `.gitignore` at the root for simplicity, though project-specific subdirectories exist.
- **Selective Wildcarding**: Use recursive globbing (`**/target/`) where appropriate to handle multi-module structures.
- **Git Cache Clear**: Use `git rm -r --cached .` followed by `git add .` to apply the new ignore list to the existing index.

## Risks / Trade-offs

- **Accidental Ignored Files**: Some developers might lose tracking of rare but necessary configuration files if the rules are too aggressive.
- **Repo History**: Large "deleted" commits in Git history for long-lived binary artifacts might remain in older commits, though current index will be cleaned.
