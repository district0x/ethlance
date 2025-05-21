#!/bin/bash
set -e

echo "===== Starting Ethlance ====="

# Create logs directory if it doesn't exist
mkdir -p logs

# Check if containers are running, start if not
if ! docker ps | grep -q ethlance-ganache-1; then
  echo "Starting infrastructure containers..."
  docker-compose -f docker-compose-simple.yml up -d
  echo "Waiting for containers to be ready..."
  sleep 10
else
  echo "Infrastructure containers already running."
fi

# Start config server in background
echo "Starting configuration server..."
node server/config-server.js > logs/config-server.log 2>&1 &
CONFIG_SERVER_PID=$!
echo $CONFIG_SERVER_PID > .config-server.pid
echo "Configuration server started with PID $CONFIG_SERVER_PID"

# Start UI
echo "Starting UI (this will keep running in the foreground)..."
echo "Access Ethlance at http://localhost:6500/index.html"
cd ui && npx shadow-cljs watch dev-ui
