## ADDED Requirements

### Requirement: environment-protection
Exclusion of all non-source files including build artifacts, local environment variables, IDE metadata, and OS-specific files from Git tracking.

#### Scenario: Update .gitignore
- **WHEN** the project `.gitignore` is missing standard patterns for Java, Node.js, Python, or IDEs.
- **THEN** it should be updated with a comprehensive list of exclusion rules.

#### Scenario: Clean Git Index
- **WHEN** files that should be ignored are already tracked in the repository.
- **THEN** they should be removed from the Git cache without deleting the local files.
