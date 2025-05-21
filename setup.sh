#!/bin/bash
set -e

echo "===== Ethlance Setup Script ====="

# Create logs directory
mkdir -p logs

echo "Installing dependencies..."
npm install

echo "Installing LESS compiler if not present..."
if ! command -v lessc &> /dev/null; then
    npm install -g less
fi

echo "Setting up infrastructure..."
docker-compose -f docker-compose-simple.yml up -d

echo "Waiting for infrastructure to be ready..."
sleep 10

echo "Deploying smart contracts..."
npx truffle migrate --network ganache --reset

echo "Compiling CSS..."
bb compile-css

echo "Creating config-server.js if it doesn't exist..."
if [ ! -f server/config-server.js ]; then
  cat > server/config-server.js << 'EOL'
const http = require('http');

// Configuration to serve
const getConfig = () => {
  return {
    "web3-provider": "ws://localhost:8545",
    "graphql-url": "http://localhost:6300/graphql",
    "ipfs-gateway": "http://localhost:8080/ipfs/"
  };
};

// Create a simple HTTP server
const server = http.createServer((req, res) => {
  // Set CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  
  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }
  
  // Only handle GET requests to /config
  if (req.method === 'GET' && req.url === '/config') {
    try {
      const config = getConfig();
      res.setHeader('Content-Type', 'application/json');
      res.writeHead(200);
      res.end(JSON.stringify(config));
    } catch (error) {
      console.error('Error serving configuration:', error);
      res.writeHead(500);
      res.end(JSON.stringify({ error: 'Internal Server Error' }));
    }
  } else if (req.method === 'GET' && req.url === '/graphql') {
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(200);
    res.end(JSON.stringify({ data: {} }));
  } else {
    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Not Found' }));
  }
});

const PORT = 6300;
server.listen(PORT, () => {
  console.log(`Configuration server running at http://localhost:${PORT}`);
});
EOL
fi

# Fix any d0x-vm references
echo "Updating configuration to use localhost instead of d0x-vm..."
find ui/src -type f -name "*.cljs" -exec sed -i '' 's/d0x-vm/localhost/g' {} \;

echo "Setup complete! Use start.sh to launch Ethlance."
