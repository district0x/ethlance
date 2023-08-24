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
      settings: {
        optimizer: {
          enabled: true,
          runs: 200
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
      gas: 6e6, // gas limit
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
    hardhat: {
      host: 'localhost',
      port: 8545,
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },


    hostia: { // Truffle Ganache GUI app running on host machine
      host: "192.168.32.1",     // Localhost (default: none)
      port: 7545,               // Standard Ethereum port (default: none)
      network_id: "5777",       // Any network (default: none)
      websockets: true,
      provider: () => new HDWalletProvider(mnemonic, `http://192.168.32.1:7545`),
    },
  },

  compilers: {
    solc: {
      version: "0.8.12",
    }
  }
}