## Why

The current `.gitignore` is insufficient, leading to Git tracking unwanted build artifacts (e.g., `backend/target/` classes), IDE-specific files (`.metadata/`, `.agent/`), and OS meta-files. This clutter makes the repository difficult to manage and can lead to merge conflicts or accidental commits of sensitive/local data.

## What Changes

- Comprehensive update to `.gitignore` to include patterns for Java/Maven, Node.js, Python, IDEs (VS Code, IntelliJ, Eclipse), and Docker/OS files.
- Orchestrate a repository "deep clean" by untracking files that are already indexed but should be ignored.

## Capabilities

### New Capabilities
- `environment-protection`: Standardized exclusion of local configuration (`.env`), temporary files, and platform-specific metadata.

### Modified Capabilities
- None

## Impact

- **Git Index**: Significant reduction in tracked files and noise in `git status`.
- **Developer Workflow**: Prevents accidental commits of local build artifacts and IDE settings.
- **CI/CD**: Cleaner builds as only source files are considered.
