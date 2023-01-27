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

# Contributing

Anyone is welcome to contribute to the ethlance project, here are some brief guidelines:

* Make sure to squash your commits
* Reference issue numbers in your pull request
* Rebase your changes on upstream (`git remote add upstream
  https://github.com/madvas/ethlance.git`) master before pushing  (`git pull --rebase upstream master`)
* Make changes in a separate well-named branch in your forked repo
  like `improve-readme`
