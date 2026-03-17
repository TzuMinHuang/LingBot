#!/bin/bash

# run-benchmarks.sh
# Centralized entry point for performance testing

BASE_URL=${1:-"http://localhost:8080"}
SCENARIO=${2:-"chat-send"}
PROFILE=${3:-"baseline"}
SCALE=${4:-"1"}

REPORTS_DIR="tests/performance/reports"
mkdir -p "$REPORTS_DIR"

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
REPORT_FILE="$REPORTS_DIR/${SCENARIO}_${PROFILE}_S${SCALE}_${TIMESTAMP}"

echo "🚀 Starting k6 benchmark..."
echo "📍 Target URL: $BASE_URL"
echo "📜 Scenario: $SCENARIO"
echo "📊 Profile: $PROFILE"
echo "🏗️  Backend Scale: $SCALE"

# Apply scaling if profile is load or stress
if [ "$SCALE" -gt 1 ]; then
  echo "⚖️  Scaling backend-service to $SCALE..."
  docker compose --profile infra --profile app scale backend-service="$SCALE"
  # Wait for healthchecks
  echo "⏳ Waiting for instances to be healthy..."
  sleep 10
fi

# Define profile options
case $PROFILE in
  baseline)
    VUS=1
    DURATION="30s"
    ;;
  load)
    VUS=10
    DURATION="2m"
    ;;
  stress)
    VUS=50
    DURATION="5m"
    ;;
  *)
    echo "❌ Unknown profile: $PROFILE. Falling back to baseline."
    VUS=1
    DURATION="30s"
    ;;
esac

# Check if k6 is installed
if ! command -v k6 &> /dev/null; then
  echo "⚠️  k6 not found. Please install it first: https://k6.io/docs/getting-started/installation/"
  exit 1
fi

# Run k6 with the reporter
k6 run \
  --env BASE_URL="$BASE_URL" \
  --vus "$VUS" \
  --duration "$DURATION" \
  --summary-export="${REPORT_FILE}.json" \
  "tests/performance/scenarios/${SCENARIO}.js"

echo "✅ Benchmark complete!"
echo "📄 JSON Report: ${REPORT_FILE}.json"
echo "📊 HTML Dashboard: ${REPORT_FILE}.html"
