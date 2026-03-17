## ADDED Requirements

### Requirement: Centralized k6 Configuration
The system SHALL provide a centralized configuration for k6 tests, including base URLs and global test options (e.g., duration, VUs).

#### Scenario: Running test with environment-specific URL
- **WHEN** a test is started with `BASE_URL=http://localhost:8080`
- **THEN** all requests within the k6 script SHALL use this address as the prefix.

### Requirement: Shared Test Utilities
The system SHALL provide a set of shared utility functions for generating test data, such as unique session IDs and randomized message content.

#### Scenario: Using session helper
- **WHEN** a k6 script needs to simulate a new user
- **THEN** it SHALL call the `generateSessionId()` helper to obtain a unique, compliant string.
