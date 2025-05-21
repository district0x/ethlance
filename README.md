# Ethlance V.2 (Newlance)

[![CircleCI](https://circleci.com/gh/district0x/ethlance/tree/newlance.svg?style=svg)](https://circleci.com/gh/district0x/ethlance/tree/newlance)

*Ethlance Version 2 is Currently in Development and is subject to
change before final release*

# Development

## Quick Start (Turn-key Setup)

We now provide a turn-key setup process for getting Ethlance running locally with minimal effort:

1. **Run the setup script:**
   ```bash
   ./setup.sh
   ```

2. **Start Ethlance:**
   ```bash
   ./start.sh
   ```

3. **Access in your browser:**
   http://localhost:6500/index.html

For complete details, see our [Detailed Setup Guide](./DETAILED_SETUP_GUIDE.md) which walks through every step of the process.

## Prerequisites

1. Node.js >= 20.18.1 (it's defined in `.tool-versions` and [asdf](https://github.com/asdf-vm/asdf-nodejs) is a good way to install it)
2. Java JDK >= 18 (for Clojure)
3. [Babashka](https://github.com/babashka/babashka#installation)
4. PostgreSQL (tested with 17.3)
5. IPFS daemon
6. Ethereum testnet (e.g. ganache)

## Running the system

Clojure gets its missing dependencies automatically during compilation.
Node.js dependencies need to be installed manually. There are 3 places for it:
1. Testnet & smart contract node deps at the project root:
  - `yarn install`
2. UI node dependencies
  - `cd ui && yarn install`
3. Server node dependencies
  - `cd server && yarn install`

Also a database needs to exist (configured in `config/server-config-dev.edn` under `:district/db` key).
Here's some SQL to do it (via the `psql` command)
```sql
CREATE DATABASE ethlance_new;
CREATE USER ethlanceuser_new WITH PASSWORD 'pass_new';
GRANT ALL PRIVILEGES ON DATABASE ethlance_new TO ethlanceuser_new;
\c ethlance_new
GRANT ALL ON SCHEMA public TO ethlanceuser_new;
ALTER DATABASE ethlance_new OWNER TO ethlanceuser_new; -- Optional
```

> _The database schema gets created automatically when server is started._

After this you can start all necessary services at once from the terminal with `overmind start`
  - the processes to be started are defined in `Procfile`
  - [Overmind](https://github.com/DarthSim/overmind)
  - *TIP* for development it's better to run the `bb run-server` separately because although shadow-cljs recompiles the code on file saves, the node process doesn't reload it and for that manual restart is required to reload the updated code.

Or you can start them one by one (check the `Procfile` for respective commands):
1. Start IPFS
  - this is optional if the `:ipfs` config points to external node
2. Start ganache `bb testnet-dev`
3. Migrate Solidity contracts to testnet: `npx truffle migrate --network ganache --reset`
4. Start server build `bb watch-server`
5. Start server (to serve the API) `bb run-server`
6. Start UI build `bb watch-ui`
  - this also starts serving the UI assets & smart contract assets on port `6500`

### IPFS Server

Might require additional configuration for CORS if IPFS is running on a different host

```bash
ipfs config --json Gateway.HTTPHeaders.Access-Control-Allow-Methods '["PUT", "GET", "POST", "OPTIONS"]'
ipfs config --json Gateway.HTTPHeaders.Access-Control-Allow-Origin '["*"]'
ipfs config --json Gateway.HTTPHeaders.Access-Control-Allow-Headers '["X-Requested-With"]'
ipfs config --json Gateway.Writable true
```

# Building & deployment

## 1. Smart contracts
The ethlance smart contracts should (for now) be deployed manually. As a result of the compilation process, the environment specific clojure files with contract addresses get written (e.g. `shared/src/ethlance/shared/smart_contracts_qa.cljs` for QA) and must be committed to git to make them available for deployment of browser & server applications.

During clojure application deployment (browser, server), the contracts must only be compiled, which will generate JSON ABI files under `<ethlance-root>/resources/public/contracts/build/`

Compilation:
1. `ETHLANCE_ENV=qa npx truffle compile` (replacing the network with one suitable for the env)
2. This generates ABI JSON files under `<ethlance-root>/resources/public/contracts/build/`
3. Server app needs to access them during runtime:
  - configured via `[:smart-contracts :contracts-build-path]`
4. UI (browser app) needs them available: should be served by Nginx (or your web server of choice)
  - get loaded from urls like `http://d0x-vm:6500/contracts/build/TestToken.json`

## 2. Server (<ethlance-root>/server)
To build Server (consists mainly of graphql API):
1. The following ENV variables need to be set:
  - `export ETHLANCE_ENV=qa`
2. Compile with `clj -A:dev:shadow-clj release dev-server`
  - will result a single file in `out/ethlance_server.js` (and accompanying `ethlance_server.js.map` source map)
3. Before running it the smart contract ABI JSON files need to be in a location defined in the EDN file `ETHLNCE_CONFIG_PATH` at EDN path `[:smart-contracts :contracts-build-path]`
  - if it's a relative path, it gets resolved in relation to where the server process gets started

Running server:
1. Earlier the server config was compiled into the generated JS file. Now it will be loaded during runtime (at application startup) from the variable specified under `:config :env-name`. As for now it is `ETHLANCE_CONFIG_PATH`.
2. Also earlier the UI (browser app) config was baked into the compiled JS file for the UI. Now it gets served via `/config` endpoint from a location pointed to ENV variable `UI_CONFIG_PATH` (contents loaded at run time from the file system). Thus, starting the server:
  - `export ETHLANCE_ENV=qa`
  - `export SERVER_CONFIG_PATH=/path/to/server-config-qa.edn `
  - `export UI_CONFIG_PATH=/path/to/ui-config-qa.edn`
  - `node out/ethlance_server.js`

## 3. Browser (<ethlance-root>/ui)
1. The following ENV variables need to be set:
  - `export ETHLANCE_ENV=qa`
2. Compile with `clj -A:dev:shadow-clj release dev-ui`
  - the generated JS file (single) will be under `<ethlance-root>/ui/resources/public/main.js`
3. To serve the web page, configure the web server to serve the files under `<ethlance-root>/ui/resources/public`
  - there are some other files, like CSS, JS and index.html that the web server needs to serve too
4. The web server must also serve the ABI JSON files generated in _1. Smart contracts_
  - for that, they can be copied from the build step or re-compiled and moved to where the web server can serve them
  - the expected HTTP path will be `/contracts/build/<ABI JSON FILE>.json` (e.g. `/contracts/build/Ethlance.json`)

# Contributing

Anyone is welcome to contribute to the ethlance project, here are some brief guidelines:

* Make sure to squash your commits
* Reference issue numbers in your pull request
* Rebase your changes on upstream (`git remote add upstream
  https://github.com/madvas/ethlance.git`) master before pushing  (`git pull --rebase upstream master`)
* Make changes in a separate well-named branch in your forked repo
  like `improve-readme`

# Overview of workflows

Here's a brief overview of the sequence of actions that different user types can take to create, find and arbiter jobs.

1. User arrives to the home page
  - http://d0x-vm:6500
  - picks which profile to create (freelancer = candidate, employer, arbiter)
2. This takes the user to the sign-up page
  - http://d0x-vm:6500/me/sign-up?tab=candidate
  - user fills in details for the roles they want to participate (candidate, employer, arbiter)
3. **Employer** (having first filled in employer data)
  - http://d0x-vm:6500/jobs/new
  - user can create new job by filling in the form
  - creating a new job requires sending a signed transaction together with the initial funds
  - smart contract method `Ethlance#createJob` is called and a new copy of `Job` contract gets deployed
4. In case the employer invited arbiters during job creation `/jobs/new`
  - First: **arbiter** must send their quote (how much they want for their service for this job)
    + ... where can they do it (one or more pages)?
    + script helper: `tx-workflow-fns-server-repl/set-quote-for-arbitration`
  - Second: **employer** must accept the quote
    + ... where can they do it (one or more pages)?
    + script helper: `tx-workflow-fns-server-repl/accept-quote-for-arbitration`
5. **Employer** can now invite candidates to participate in the new job
  - **NB!** implement & provide link
  - results in `Job#addCandidate` contract send
    + script helper: `tx-workflow-fns-server-repl/add-candidate`
6. **Candidate** can search for available jobs
  - http://d0x-vm:6500/jobs
7. **Candidate** chooses a job of interest from search results. To work on it, needs to send proposal
  - http://d0x-vm:6500/jobs/detail/0x1A2f3f7739A52F5bDCc909A8d42e96Cd8f9f4D30
  - as a result `JobStory` is created (DB model to track this user's interaction with this job)
8. All users can view job contract page at
  - http://d0x-vm:6500/jobs/contract/1
  - send messages(all), raise disputes(candidate), resolve disputes(arbiter), leave feedback(all)
9. **Candidate** can create invoices (for any of their jobs)
  - http://d0x-vm:6500/invoices/new
10. **Candidate** can then raise dispute
  - after raising dispute, arbiter can resolve it on the same page (http://d0x-vm:6500/invoices/new)
    + resolving dispute results in tokens getting transferred according to the resolution amount
11. **Employer** can pay invoices
  - ... (add link, implement if needed)
