# Ethlance V.2 (Newlance)

*Ethlance Version 2 is Currently in Development and is subject to
change before final release*

# Development

## Prerequisites

* [Leiningen](https://leiningen.org/)

* [solc](https://www.npmjs.com/package/solc)

* [ganache-cli](https://github.com/trufflesuite/ganache-cli)

* make
  * Note: Windows users can use Msys for build essentials
	(Untested)

## Setup Backend

Backend development requires:

* instance of a figwheel server build

* an attached node server to the figwheel server

* a solidity contract compiler (lein-solc)

* a test net (ganache-cli)

### Quickstart

Terminal 1 (cljs repl):

```bash
make fig-dev-server
```

Terminal 2 (node server):

```bash
# Wait for fig-dev-server prompt...
make dev-server
```

Terminal 3 (testnet):
```bash
ganache-cli
```

Terminal 4 (solc):
```bash
make watch-contracts
```

Terminal 1 (server repl):
```clojure
(redeploy) ;; ...
;; Wait for message informing of completion
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

### Smart Contract Deployment

If all of the previous sections are completed, we can perform a smart
contract deployment on the testnet through the fig-dev-server
CLJS-REPL.
 
While in the Figwheel Server CLJS REPL, type:

```clojure
(redeploy)
```

You should see activity in the `ganache-cli` testnet server, and a
message should inform you when the deployment has finished.

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

# Contributing

Anyone is welcome to contribute to the ethlance project, here are some brief guidelines:

* Make sure to squash your commits
* Reference issue numbers in your pull request
* Rebase your changes on upstream (`git remote add upstream
  https://github.com/madvas/ethlance.git`) master before pushing  (`git pull --rebase upstream master`)
* Make changes in a separate well-named branch in your forked repo
  like `improve-readme`
