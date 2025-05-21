# Ethlance Detailed Setup Guide

This guide provides step-by-step instructions for setting up Ethlance locally for development. Whether you're a new contributor or just want to explore the platform, these instructions will help you get up and running with minimal fuss.

## What is Ethlance?

Ethlance is a decentralized freelance marketplace built on Ethereum. It allows freelancers and employers to connect, collaborate, and transact directly using smart contracts without any middlemen.

## Setup Overview

After following this guide, you'll have:
- A local Ethereum blockchain (Ganache) with Ethlance contracts deployed
- A local IPFS node for decentralized storage
- A PostgreSQL database 
- The Ethlance UI running locally
- A configuration server to support the UI

**Estimated setup time:** 15-30 minutes, depending on download speeds.

## Prerequisites

Before starting, ensure you have the following installed:

| Tool | Purpose | Installation Command | Verification |
|------|---------|---------------------|-------------|
| Git | Version control | [Git installation guide](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) | `git --version` |
| Docker & Docker Compose | Running backend services | [Docker Desktop](https://www.docker.com/products/docker-desktop/) | `docker --version && docker-compose --version` |
| Node.js & npm | JavaScript runtime and package manager | [Node.js installer](https://nodejs.org/) or via nvm: `nvm install 16` | `node -v && npm -v` |
| SDKMAN! | Java version management | `curl -s "https://get.sdkman.io" \| bash` then `source "$HOME/.sdkman/bin/sdkman-init.sh"` | `sdk version` |
| Java (JDK) | Required by Clojure | Via SDKMAN: `sdk install java 21-tem` | `java -version` |
| Clojure | Programming language | Via SDKMAN: `sdk install clojure` | `clojure -e '(println "Hello")'` |
| Babashka | Clojure scripting tool | macOS: `brew install babashka/tap/babashka`<br>Other: [Installation guide](https://github.com/babashka/babashka#installation) | `bb --version` |
| LESS Compiler | CSS preprocessing | `npm install -g less` | `lessc --version` |
| Truffle | Smart contract deployment | `npm install -g truffle` | `truffle version` |

## Quick Start (Automated Setup)

We've provided scripts to automate the entire setup process:

1. **Clone the repository:**
   ```bash
   git clone https://github.com/district0x/ethlance.git
   cd ethlance
   ```

2. **Run the setup script:**
   ```bash
   ./setup.sh
   ```
   This script:
   - Installs dependencies
   - Starts Docker containers (Ganache, IPFS, PostgreSQL)
   - Deploys smart contracts to Ganache
   - Compiles CSS
   - Creates the configuration server
   - Fixes any hostname issues in the codebase

3. **Start Ethlance:**
   ```bash
   ./start.sh
   ```
   This script:
   - Ensures infrastructure containers are running
   - Starts the configuration server in the background
   - Starts the UI development server (keeps running in the foreground)

4. **Access Ethlance:**
   Open your browser and navigate to http://localhost:6500/index.html

5. **When finished, stop Ethlance:**
   ```bash
   ./stop.sh
   ```
   This will stop the UI and configuration servers, and optionally the Docker containers.

## Manual Setup (Step-by-Step)

If you prefer to understand each step or if the automated setup encounters issues, follow these steps:

### 1. Clone the Repository

```bash
git clone https://github.com/district0x/ethlance.git
cd ethlance
```

### 2. Start Backend Infrastructure

```bash
docker-compose -f docker-compose-simple.yml up -d
```

This starts:
- Ganache (Ethereum blockchain) on port 8545
- IPFS on ports 5001/8080
- PostgreSQL on port 5432

Verify containers are running:
```bash
docker ps | grep ethlance
```

### 3. Deploy Smart Contracts

```bash
npx truffle migrate --network ganache --reset
```

This deploys all necessary contracts to your local Ganache instance.

### 4. Compile CSS

```bash
bb compile-css
```

### 5. Create and Start the Configuration Server

Create the file `server/config-server.js` with this content:

```javascript
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
```

Run the server in a new terminal:
```bash
node server/config-server.js
```

### 6. Update Host References in Configuration Files

Replace all instances of `d0x-vm` with `localhost`:

```bash
find ui/src -type f -name "*.cljs" -exec sed -i '' 's/d0x-vm/localhost/g' {} \;
```

### 7. Start the UI

In a new terminal, run:
```bash
cd ui
npm install
npx shadow-cljs watch dev-ui
```

### 8. Access Ethlance

Open your browser and navigate to: http://localhost:6500/index.html

## Project Structure

Here's a brief overview of the main components:

- `ui/`: Frontend code (ClojureScript, React)
- `server/`: Backend server code (Clojure)
- `shared/`: Code shared between UI and server
- `resources/`: Static assets and configuration
- `truffle/`: Ethereum smart contracts and deployment configuration
- `contracts/`: Solidity smart contracts

## Basic Usage Guide

Once Ethlance is running, you can:

### Create a Test Account
1. Connect with MetaMask to your local Ganache (http://localhost:8545)
2. Import a Ganache account using its private key (found in the Ganache UI or logs)
3. Register as either a freelancer or employer

### Post a Job
1. Login as an employer
2. Click "Post a Job"
3. Fill in job details and submit

### Apply for a Job
1. Login as a freelancer
2. Browse available jobs
3. Apply to jobs of interest

## Troubleshooting

### Common Issues

#### "Cannot connect to Ethereum node"
- Ensure Ganache container is running: `docker ps | grep ganache`
- Check if Ganache is accessible: `curl -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[],"id":1}' http://localhost:8545`

#### Infinite Loading Spinner
- Check browser console for errors
- Ensure the config server is running
- Verify the UI is pointing to localhost, not d0x-vm

#### "Port already in use" errors
- Check what's using the port: `lsof -i :<port_number>`
- Stop the conflicting process or change the port in configuration

#### UI Not Loading
- Check if `shadow-cljs` is running properly
- Verify CSS has been compiled with `bb compile-css`
- Check browser console for specific errors

### Port Reference

| Service | Port | Notes |
|---------|------|-------|
| Ganache | 8545 | Local Ethereum blockchain |
| IPFS API | 5001 | IPFS node API |
| IPFS Gateway | 8080 | For accessing IPFS content |
| PostgreSQL | 5432 | Database |
| Config Server | 6300 | Provides configuration to UI |
| UI Dev Server | 6500 | Serves the Ethlance UI |

## Contributing

If you're interested in contributing to Ethlance, please:

1. Ensure all code is properly tested
2. Follow the existing code style
3. Document any new features or changes
4. Submit a pull request with a clear description of changes

## Additional Resources

- [Clojure Documentation](https://clojuredocs.org/)
- [ClojureScript Documentation](https://clojurescript.org/)
- [Ethereum Development Documentation](https://ethereum.org/developers/)
- [Solidity Documentation](https://docs.soliditylang.org/)
- [Re-frame Documentation](https://github.com/day8/re-frame)

---

If you encounter any issues not covered in this guide, please open an issue on GitHub.
