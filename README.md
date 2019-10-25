# Ethlance V.2 (Newlance)

![](https://travis-ci.org/district0x/ethlance.svg?branch=newlance)

*Ethlance Version 2 is Currently in Development and is subject to
change before final release*

# Ethlance Feature Spec

* The app allows to sign up as 3 different types of users: Employers, Candidates, Arbiters.
* Employers post jobs, invite candidates and arbiters, pay invoices. Candidates apply for a job, receive payments. Arbiters resolve job disputes, receive payments.
* All 3 types of users can be registered from the same Ethereum address.
* When creating job, an employer specifies following options:
  * Employer chooses if it's regular job or a bounty. For a regular job, candidates submit job proposal without any prior work done for that job. For a bounty, candidates submit finished work without having to submit job proposal first.
  * Employer fills out job category, required skills, job description
  * Employer chooses form of a payment. It can be Ether or arbitrary ERC20 token, given the token address.
  * Employers choose whether he wants to add an arbiter for his job. If yes, he specifies fixed amount of Ether he's willing to pay for arbiter's services. After that he can choose multiple arbiters from a list, who will be invited for the job. An arbiter who accepts the invitation first, will be resolving a job dispute, in case there's a dispute. Arbiter receives his fee immediately after he accepts the invitation.
  * For a bounty, employer also specifies amount of Ether or token that will be paid out for successful work submissions. This amount will be transferred from employer's wallet to a job contract at the time of job creation.
  * For a regular job, employer has option to transfer any amount of Ether or token to a job contract at the time of job creation. This will help increase credibility of a job, since once funds are help by job contract, an arbiter has priviledges to transfer funds to condidates.
* When proposing for a job, a candidate specifies following options:
  * For regular job, he specifies his rate and writes proposal text.
  * For bounty, he specifies fixed amount, which he claims for submitting work, writes explanation and attaches files if needed.
 * Once job proposal or bounty work are submitted, a Job Contract page gets available for all 3 involved users: employer, candidate, arbiter. Job Contract page has form of a message conversation between involved users. These are events on which a new message can be added into job contract conversation:
   * Employer invites candidate to apply for the job
   * Candidate applies for the job
   * Candidate submits work done for the bounty
   * Employer hires a candidate
   * Employer/Candidate/Arbiter submits a message
   * Candidate submits an invoice for accomplished work
   * Employer pays for the invoice
   * Employer/Candidate raises a dispute
   * Arbiter resolves a dispute. During dispute resolution, the arbiter specifies amounts that will be sent to candidate and employer, from funds held in a job contract.
   * Employer/Candidate/Arbiter leaves feedback for Employer/Candidate/Arbiter
* The app provides option for anyone to sponsor a job, which means transferring funds into a job contract. In case user changes his mind about sponsoring, he can refund himself his contribution, in case it wasn't spent yet.
* For all users, the app provides list of jobs/candidates/arbiters with various filtering options as specified in designs.
* For employers, the app provides following lists:
  * My Jobs - Jobs created by an employer.
  * My Contracts - Job Contracts the employer is involved in.
  * My Invoices - Invoices, which the employer paid or is supposed to pay.
* For candidate, the app provides following lists:
  * My Jobs - Jobs candidate applied for
  * My Contracts - Job Contracts the candidate is involved in.
  * My Invoices - Invoices, which candidate sent to an employer
* For arbiter, the app provides following lists:
  * My Disputes - Disputes the arbiter is or was involved in.
  * My Contracts - Job Contracts the arbiter is involved in.
  * My Invoices - Invoices with payments arbiter received for dispute resolution.

### Integration of StandardBounties
Ethlance uses [StandardBounties.sol](https://github.com/Bounties-Network/StandardBounties/blob/master/contracts/StandardBounties.sol) smart-contract for almost all of its operations. In addition to StandardBounties, it uses EthlanceBountyIssuer smart-contract, which helps managing arbiters for a job. 
Following list explains how StandardBounties and EthlanceBountyIssuer smart-contract functions integrate into Ethlance: 
* `EthlanceBountyIssuer::issueAndContribute` - This function calls `StandardBounties::issueAndContribute`, passing as issuers address of itself and sender's address, so later this contract has priviledges to add a approver (arbiter) to StandardBounties job. It also stores addresses of invited arbiters and arbiter's fee.
* `EthlanceBountyIssuer::inviteArbiters` - This function is for inviting more arbiters, in case nobody accepted in the initial round of invites.
* `EthlanceBountyIssuer::acceptArbiterInvitation` - Arbiter calls this function to accept an invitation. If he's first, who accepted invitation for a particular job, it'll transfer fee to him and add him as an arbiter for the job. This function calls `StandardBounties::addApprovers`.
* `StandardBounties::issueAndContribute` - This function will be called only by `EthlanceBountyIssuer::issueAndContribute`, so that contract has priviledges to manage approvers in StandardBounties contract.
* `StandardBounties::fulfillBounty` - This function is called by candidate when he sends invoice for accomplished work in regular job. It's also called for a bounty, when candidate sends work submission. Note, in this function user cannot submit the amount he claims for work into the smart-contract. The amount will be saved into Ethlance database and this number will be later used in `StandardBounties::acceptFulfillment` call.
* `StandardBounties::acceptFulfillment` - This function is either called by an employer when he pays out the invoice or by an arbiter (as an approver) when he resolves a dispute. It accepts parameter `_tokenAmounts`, where arbitrary amounts can be specified.
* `StandardBounties::contribute` - This function is called by anyone who'd like to sponsor particular job.
* `StandardBounties::refundContribution` - This function is called by user who sponsored a job before and would like to get his contribution back.
* `StandardBounties::addApprovers` - This function is called by `EthlanceBountyIssuer::acceptArbiterInvitation`, so an invited arbiter gets added as an approver for a bounty in StandardBounties.


# Development

## Prerequisites

* [NodeJS](https://nodejs.org) (Latest LTS Version)

* [Leiningen](https://leiningen.org/)

* [solc](https://www.npmjs.com/package/solc)

* [ganache-cli](https://github.com/trufflesuite/ganache-cli)

* [ipfs](https://docs.ipfs.io/introduction/install/)

* [truffle](https://github.com/trufflesuite/truffle)

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

# Testing One-touch

To run all of the tests in a standalone test runner, you must first
build the solidity contracts, run an instance of the testnet server,
and an instance of the IPFS daemon. The test runner can be run via:

```bash
make clean deploy test
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
