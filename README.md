# Ethlance V.2 (Newlance)

[![CircleCI](https://circleci.com/gh/district0x/ethlance/tree/newlance.svg?style=svg)](https://circleci.com/gh/district0x/ethlance/tree/newlance)

*Ethlance Version 2 is Currently in Development and is subject to
change before final release*

# Development

## Prerequisites

1. Node.js >= 16.15.1
2. Java JDK >= 18 (for Clojure)
3. Babashka
4. PostgreSQL (tested with 14.6)
5. IPFS daemon
6. Ethereum testnet (e.g. ganache)

## Running the system

1. Start IPFS
2. Start ganache
3. Migrate Solidity contracts to testnet: `npx truffle migrate --network ganache --reset`
3. Start server build `bb watch-server`
4. Start server (to serve the API) `bb run-server`
4. Start UI build `bb watch-ui`
  - this also starts serving the UI assets & smart contract assets on port `6500`

### First steps, showing example data

In order for the front-end to be able to have the JWT token (kept in LocalStorage), you must sign a transaction. Currently it can be done manually. Open REPL for UI:
```
lein repl :connect 54200
(shadow/repl :dev-ui)
(in-ns 'ethlance.ui.event.sign-in)
(re/dispatch [:user/sign-in])
```
  - this will show a pop up and using MetaMask you can create a transaction
  - after doing this successfully the UI graphql requests will have proper `Authorization: Bearer ...` header

Then to generate some example data you can use server REPL:
```
lein repl :connect 54100
(shadow/repl :dev-server)
(in-ns 'tests.graphql.generator)
(generate-for-address "0xafcf1a2bc71acf041c93012a2e552e31026dfeab")
```
  - for that the test namespace must be included in the server build (e.g. by adding `[tests.graphql.generator :as test-data-generator]` to `ethlance.server.core`)
  - alternatively you can submit the data manually through the forms

## Tips & troubleshooting

### Postgres setup

Start postgres console `psql -d postgres`

```sql
CREATE USER ethlanceuser WITH ENCRYPTED PASSWORD 'pass';
CREATE DATABASE ethlance WITH OWNER ethlanceuser;
-- alternatively if you created the database earlier, give access with:
GRANT ALL PRIVILEGES On ethlance TO ethlanceuser;
```

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
