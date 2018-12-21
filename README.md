# Ethlance V.2 (Newlance)

*Ethlance Version 2 is Currently in Development and is subject to
change before final release*

# Development

## Prerequisites

* [Leiningen](https://leiningen.org/)

* [solc](https://www.npmjs.com/package/solc)

* [ganache-cli](https://github.com/trufflesuite/ganache-cli)

* [ipfs](https://docs.ipfs.io/introduction/install/)

* make
  * Note: Windows users can use Msys for build essentials
	(Untested)


Run `make check` to determine whether you are missing any prerequisites


## Setup Backend

Backend development requires:

* instance of a figwheel server build

* an attached node server to the figwheel server

* a solidity contract compiler (lein-solc)

* an local IPFS daemon with properly configured CORS privileges (ipfs)

* a test net (ganache-cli)

### Quickstart

Terminal 1 (cljs repl):

```bash
make fig-dev-server
```

Terminal 2 (node server):

```bash
# Wait for fig-dev-server prompt: 'Prompt will show when Figwheel connects to your application'
make dev-server
```

Terminal 3 (testnet):
```bash
make testnet
```

Terminal 4 (solc):
```bash
make watch-contracts
```

Terminal 5 (ipfs):
```bash
make ipfs
```


Terminal 1 (server repl):
```clojure
(start) 
	    ;; By default, this will deploy the smart contracts, generate users
	    ;; and scenarios, and synchronize the results within the SQLite database.

(help)  ;; To review additional commands
```

### Initial Setup

*Note: All instructions start in the root of the project directory*

If this is a first time setup, or you accidentally ran `make
clean-all`, you are required to re-install additional node
dependencies with:

```bash
make deps
```

If you're having issues with your environment, you can run this command:

```bash
make clean-all deps
```

### Figwheel Server and Node Server Instance

*Note: All instructions start in the root of the project directory*

Open two terminals. In the first terminal, type:

```bash
$ make fig-dev-server
```

After a short while, the build will prompt for a connection. 

In the second terminal, type:

```bash
$ make dev-server
```

The the figwheel server should establish a connection with the node
development server, and a CLJS repl should be available in the first
terminal.

### Testnet Server

Our local development environment requires a testnet in order to run
smart contracts. Ethlance Development uses ganache-cli with a default
configuration.

```bash
$ ganache-cli # Run in a separate terminal
```

### IPFS Server

IPFS is an essential technology in ensuring that large blobs of data
can be stored in a decentralized manner. After installing the `ipfs`
commandline tool, you need to start up a daemon for development

```bash
ipfs daemon
```

**Note**: Might require additional configuration for CORS

```bash
ipfs config --json Gateway.HTTPHeaders.Access-Control-Allow-Methods '["PUT", "GET", "POST", "OPTIONS"]'
ipfs config --json Gateway.HTTPHeaders.Access-Control-Allow-Origin '["*"]'
ipfs config --json Gateway.HTTPHeaders.Access-Control-Allow-Headers '["X-Requested-With"]'
ipfs config --json Gateway.Writable true
```

### Smart Contract Compilation

*Note: All instructions start in the root of the project directory*

Ethlance Development uses `lein-solc` for smart contract
compilation. In a separate terminal, run:

```bash
$ make watch-contracts
```

This will compile all contracts located in
`./resources/public/contracts/src` and re-compile any contracts that are
changed during development.

### Smart Contract Deployment, User / Scenario Generation, Syncing

If all of the previous sections are completed, we can perform a smart
contract deployment on the testnet through the fig-dev-server
CLJS-REPL.
 
While in the Figwheel Server CLJS REPL, type:

```clojure
(start) ;; Reloaded Lifecycle
```

You should see activity in the `ganache-cli` testnet server.

# Deployment

Ethlance product can be prepared for deployment by running:

```bash
$ make build
```

This compiles everything and places it in the *./dist* folder

After building, the production build can be run:

```bash
$ make run
```

# Testing

To run all of the tests in a standalone test runner, you must first
build the solidity contracts, run an instance of the testnet server,
and an instance of the IPFS daemon. The test runner can be run via:

```bash
make test
```

## Reloaded Workflow Testing

Tests can also be run from within the development environment

```clojure
(run-tests)
```

If you are making changes to the solidity contracts, it will require
you to reset the testnet to ensure redeployment

```clojure
(run-tests :reset? true)
```

To resume a normal development environment, it will require you to
restart the reloaded workflow

```bash
(reset)
```


## Instrumentation

The ethlance backend makes use of `clojure.spec.alpha` to perform
instrumentation. For development, instrumentation is disabled, but it
can be enabled by running `(enable-instrumentation!)` in the repl.

**Note that the test runner has instrumentation enabled by default.**


## Individual Testing

Individual Tests can also be defined, and several have been defined
within `./dev/server/cljs/tests.cljs`. Tests that use the testnet take
a significant amount of time to run, so running specific tests within
the development environment for troubleshooting issues is essential.

ex.

```clojure
(tests/run-invoice-tests)
```

In a similar manner, if solidity contracts are modified before running
the tests, it is required that you reset the testnet

```clojure
(reset-testnet!)(tests/run-invoice-tests)
```

# Contributing

Anyone is welcome to contribute to the ethlance project, here are some brief guidelines:

* Make sure to squash your commits
* Reference issue numbers in your pull request
* Rebase your changes on upstream (`git remote add upstream
  https://github.com/madvas/ethlance.git`) master before pushing  (`git pull --rebase upstream master`)
* Make changes in a separate well-named branch in your forked repo
  like `improve-readme`
