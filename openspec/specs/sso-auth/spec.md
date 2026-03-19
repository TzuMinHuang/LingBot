## ADDED Requirements

### Requirement: Enterprise Integration and Session Lifecycle
The system SHALL integrate with an enterprise SSO provider to secure all API accesses and establish a definitive `user_id`.

#### Scenario: User authenticates successfully
- **WHEN** a user logs in via the enterprise SSO callback
- **THEN** the system SHALL issue a secure JWT and bind all subsequent SSE and HTTP requests to that user's identity

#### Scenario: Token expiration
- **WHEN** a user's JWT token expires mid-stream
- **THEN** the API Gateway SHALL reject new tasks but gracefully close existing SSE streams with a generic error code triggering a frontend re-auth flow
