# Ethlance V.2 (Newlance)

[![CircleCI](https://circleci.com/gh/district0x/ethlance/tree/newlance.svg?style=svg)](https://circleci.com/gh/district0x/ethlance/tree/newlance)

*Ethlance Version 2 is Currently in Development and is subject to
change before final release*


# Development

## Prerequisites

* [NodeJS](https://nodejs.org) (Latest LTS Version)

* [Leiningen](https://leiningen.org/)

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

### Quickstart Server

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

Terminal 4 (truffle):
```bash
make deploy
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

### Quickstart Browser

Terminal 1 (cljs repl):

```bash
make fig-dev-ui
```

Terminal 2 (LESS Compiler):

```bash
make build-css watch-css
```

Open Browser to http://localhost:6500

### Initial Setup

*Note: All instructions start in the root of the project directory*

If this is a first time setup, or you accidentally ran `make
clean-all`, you are required to re-install additional node
dependencies with:

**Tested with GCC 6 & GCC 7, does not appear to work with GCC 8**

```bash
make deps
```

If you're having issues with your environment, you can run this command:

```bash
make clean-all deps
```

#### Additional Troubleshooting

- Make sure you are using NodeJS LTS Version. (Latest LTS Version is
  v10.16.3 as of this posting)

- GCC 8+ do not work with some of the district libraries. This might
  change in the future. Please use GCC Version 6, or GCC Version 7.

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
$ make testnet # Run in a separate terminal
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

# Testing One-touch

To run all of the tests in a standalone test runner, you must first
build the solidity contracts, run an instance of the testnet server,
and an instance of the IPFS daemon. The test runner can be run via:

```bash
make clean deploy test
```

# Contributing

Anyone is welcome to contribute to the ethlance project, here are some brief guidelines:

* Make sure to squash your commits
* Reference issue numbers in your pull request
* Rebase your changes on upstream (`git remote add upstream
  https://github.com/madvas/ethlance.git`) master before pushing  (`git pull --rebase upstream master`)
* Make changes in a separate well-named branch in your forked repo
  like `improve-readme`
