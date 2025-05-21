#!/bin/bash
echo "===== Stopping Ethlance ====="

# Stop config server
if [ -f .config-server.pid ]; then
  PID=$(cat .config-server.pid)
  echo "Stopping configuration server (PID $PID)..."
  kill $PID 2>/dev/null || true
  rm .config-server.pid
else
  echo "No configuration server PID file found."
fi

# Stop UI server (shadow-cljs)
echo "Stopping any running shadow-cljs processes..."
pkill -f "shadow-cljs" 2>/dev/null || true

# Ask about stopping containers
read -p "Stop infrastructure containers too? (y/n) " STOP_CONTAINERS
if [ "$STOP_CONTAINERS" = "y" ]; then
  echo "Stopping infrastructure containers..."
  docker-compose -f docker-compose-simple.yml down
else
  echo "Infrastructure containers left running."
fi

echo "Ethlance stopped."
