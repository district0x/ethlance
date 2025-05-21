const http = require('http');
const fs = require('fs');
const path = require('path');

// Read the configuration file
const configPath = path.join(__dirname, '..', 'config', 'ui-config-dev.edn');
const configData = fs.readFileSync(configPath, 'utf8');

// Instead of trying to parse EDN, we'll create a hardcoded JSON object with the correct configuration
const getConfig = () => {
  return {
    "web3-provider": "ws://localhost:8545",
    "graphql-url": "http://localhost:6300/graphql",
    "ipfs-gateway": "http://localhost:8080/ipfs/"
  };
};

// Create a simple HTTP server
const server = http.createServer((req, res) => {
  // Set CORS headers to allow requests from any origin
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  
  if (req.method === 'OPTIONS') {
    // Handle preflight requests
    res.writeHead(204);
    res.end();
    return;
  }
  
  // Only handle GET requests to /config
  if (req.method === 'GET' && req.url === '/config') {
    try {
      // Get the configuration
      const config = getConfig();
      
      // Set response headers
      res.setHeader('Content-Type', 'application/json');
      res.writeHead(200);
      
      // Send the configuration
      res.end(JSON.stringify(config));
    } catch (error) {
      console.error('Error serving configuration:', error);
      res.writeHead(500);
      res.end(JSON.stringify({ error: 'Internal Server Error' }));
    }
  } else if (req.method === 'GET' && req.url === '/graphql') {
    // Simple mock response for GraphQL endpoint
    res.setHeader('Content-Type', 'application/json');
    res.writeHead(200);
    res.end(JSON.stringify({ data: {} }));
  } else {
    // Handle 404 for other routes
    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Not Found' }));
  }
});

// Start the server on port 6300
const PORT = 6300;
server.listen(PORT, () => {
  console.log(`Configuration server running at http://localhost:${PORT}`);
});
