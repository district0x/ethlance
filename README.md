# ethlance

[![Build Status](https://travis-ci.org/district0x/ethlance.svg?branch=master)](https://travis-ci.org/district0x/ethlance)

Repository for [ethlance.com](http://ethlance.com)

Ethlance is the first job market platform written in [ClojureScript](https://clojurescript.org/) and [Solidity](https://solidity.readthedocs.io/en/develop/) working completely on the [Ethereum](https://ethereum.org/) blockchain with 0% service fees.

Ethereum Smart Contracts are at `/resources/public/contracts/src`.

# Using Ethlance

## MetaMask

1. Download the [MetaMask](https://metamask.io/) Chrome extension
2. Create a wallet and fund it with some [Ether](https://ethereum.stackexchange.com/questions/1915/how-do-i-buy-ethereum-with-usd)
3. Go on [Ethlance](http://ethlance.com/)

# Running on localhost

Following instructions assume you're familiar with the [Clojure](https://clojure.org/) programming language and have [lein](https://leiningen.org/) installed on your machine.

To start autocompiling smart contracts (requires [solc](https://github.com/ethereum/solidity) installed):
```bash
lein auto compile-solidity
```

Start [testrpc](https://github.com/ethereumjs/testrpc)
```bash
testrpc --port 8549
```

Start Clojurescript browser REPL, first start a clojure repl
```
lein repl
```

When a clojure prompt is present, type:
```
(start-ui!)
```
The clojurescript repl will appear once you navigate to http:://localhost:6229


See `ethlance.el` on how to run the above commands in Emacs via `ethlance-jack-in` and `ethlance-start`.

Make sure in [ethlance.db/default-db](https://github.com/madvas/ethlance/blob/master/src/cljs/ethlance/db.cljs) you have the following configuration:
```clojure
:load-node-addresses? true
:node-url "http://localhost:8549"
```
Visit [localhost:6229](http://localhost:6229/) in your browser without MetaMask. I use the Chrome Incognito window. 

To redeploy all smart contracts, run the following in REPL.
```clojure
(in-ns 'ethlance.events)
(dispatch [:reinitialize])
```
After you see in browser console all contracts have been deployed, refresh the page.

To redeploy only single or few specific smart contracts run:
```clojure
;; Redeploys and hot swaps EthlanceUser smart contract. No need to refresh page.
(dispatch [:reinitialize [:ethlance-user]])
```

To build advanced compilation run:
```clojure
lein clean && lein cljsbuild once min
```

# Contributing

Anyone is welcome to contribute to the ethlance project, here are some brief guidelines:

* Squash commits
* Reference issue numbers in your pull request
* Rebase your changes on upstream (```git remote add upstream https://github.com/madvas/ethlance.git```) master before pushing (```git pull --rebase upstream master```)
* Make changes in a separate well-named branch in your forked repo like ```improve-readme```
