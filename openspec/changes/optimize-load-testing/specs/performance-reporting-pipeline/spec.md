## ADDED Requirements

### Requirement: Automated HTML Reporting
The performance testing suite SHALL automatically generate a visual HTML report summarizing the results of a test execution.

#### Scenario: Post-test report generation
- **WHEN** the `run-benchmarks.sh` script completes a k6 execution
- **THEN** a new `.html` file SHALL exist in the `reports/` directory containing charts of throughput and errors.

### Requirement: Historical Metric Tracking
The system SHALL export structured JSON metrics including error counts and percentile rankings (P50, P90, P95, P99).

#### Scenario: Metrics export for analysis
- **WHEN** a test run finishes
- **THEN** a `results.json` file SHALL be created with exact millisecond values for all tracked latency metrics.
