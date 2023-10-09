'use strict';
const ETHLANCE_ENV = process.env.ETHLANCE_ENV || "dev";


const smartContractsPaths = {
  "dev" : '/shared/src/ethlance/shared/smart_contracts_dev.cljs',
  "qa" : '/shared/src/ethlance/shared/smart_contracts_qa.cljs',
  "prod" :'/shared/src/ethlance/shared/smart_contracts_prod.cljs'
};


let parameters = {
  "qa" : {

  },

  "prod" : {

  }
};


parameters.dev = parameters.qa;


const HDWalletProvider = require('@truffle/hdwallet-provider');
const mnemonic = "easy leave proof verb wait patient fringe laptop intact opera slab shine";

module.exports = {
  env: ETHLANCE_ENV,
  smart_contracts_path: __dirname + smartContractsPaths[ETHLANCE_ENV],
  contracts_directory: __dirname + "/contracts",
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters [ETHLANCE_ENV],

  compilers: {
    solc: {
      version: "0.8.21",
      settings: {
        optimizer: {
          enabled: true,
          runs: 1
        }
      }
    }
  },

  networks: {
    development: {
      port: 9545,
      host: "0.0.0.0",
      gas: 2 * 70000,
      network_id: "*"
    },

    ganache: {
      host: 'localhost',
      port: 8549,
      from: "0xeba108B12593336bBa461b8a6e7DC5A4b597Bc7E", // 6th address
      gas: 10e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },

    "ganache-test": {
      host: 'localhost',
      port: 8550,
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },

    "ethlance.mad.is-testnet": {
      host: 'ethlance.mad.is',
      port: 8545,
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*',
      from: "0xeba108B12593336bBa461b8a6e7DC5A4b597Bc7E" // 6) address
    }
  }
}
