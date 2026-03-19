## ADDED Requirements

### Requirement: High Concurrency SSE Simulation
The load testing infrastructure SHALL stress the backend's connection handling capacity.

#### Scenario: 1000 concurrent user spike
- **WHEN** the k6 script ramps up to 1000 Virtual Users establishing SSE connections simultaneously
- **THEN** the API Gateway SHALL NOT drop connections, and CPU/Memory metrics MUST remain within defined sustainable limits.

### Requirement: Chaos and Recovery Testing
The infrastructure SHALL validate the system's ability to gracefully recover from network instability.

#### Scenario: Random connection drops during active streaming
- **WHEN** the k6 script abruptly terminates an SSE connection while an LLM task is mid-generation
- **THEN** the test SHALL verify that the LLM Worker safely persists the remaining chunks to the database and correctly cleans up the Redis Stream pending entries.
