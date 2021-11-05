'use strict';
const ETHLANCE_ENV = process.env.ETHLANCE_ENV || "dev";


const smartContractsPaths = {
  "dev" : '/src/ethlance/shared/smart_contracts_dev.cljs',
  "qa" : '/src/ethlance/shared/smart_contracts_qa.cljs',
  "prod" :'/src/ethlance/shared/smart_contracts_prod.cljs'
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
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },

    hostia: { // Truffle Ganache GUI app running on host machine
      host: "192.168.12.1",     // Localhost (default: none)
      port: 7545,            // Standard Ethereum port (default: none)
      network_id: "5777",       // Any network (default: none)
      provider: () => new HDWalletProvider(mnemonic, `http://192.168.12.1:7545`),
    },
  },

  compilers: {
    solc: {
      version: "0.8.4",
    }
  }
}
