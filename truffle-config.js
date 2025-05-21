'use strict';
const ETHLANCE_ENV = process.env.ETHLANCE_ENV || "dev";
const ETHLANCE_MNEMONIC = process.env.ETHLANCE_MNEMONIC;
const ETHLANCE_ETH_NODE_ADDRESS = process.env.ETHLANCE_ETH_NODE_ADDRESS;
const ETHLANCE_DEPLOYER_ADDRESS = process.env.ETHLANCE_DEPLOYER_ADDRESS
const ETHLANCE_DEPLOY_SEED = process.env.ETHLANCE_DEPLOY_SEED
const ETHLANCE_INFURA_API_KEY = process.env.ETHLANCE_INFURA_API_KEY || "fc19514210574bf580fbc13f58f65819";

const smartContractsPaths = {
  "dev" : '/shared/src/ethlance/shared/smart_contracts_dev.cljs',
  "qa" : '/shared/src/ethlance/shared/smart_contracts_qa.cljs',
  "qa-base" : '/shared/src/ethlance/shared/smart_contracts_qa_base.cljs',
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
const DEFAULT_DEV_MNEMONIC = "easy leave proof verb wait patient fringe laptop intact opera slab shine";
const mnemonic = ETHLANCE_MNEMONIC || DEFAULT_DEV_MNEMONIC;

module.exports = {
  env: ETHLANCE_ENV,
  smart_contracts_path: __dirname + smartContractsPaths[ETHLANCE_ENV],
  contracts_directory: __dirname + "/contracts",
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters [ETHLANCE_ENV],

  compilers: {
    solc: {
      version: "0.8.17",
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
      provider: function () {
        return new HDWalletProvider(DEFAULT_DEV_MNEMONIC, "http://localhost:8545")},
      from: "0x59a7930bD5fD51714a4cEa6F74aDE63B5F4A37F4", // First account from our Ganache instance
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

    "base-sepolia": {
      provider: function () {
        return new HDWalletProvider({mnemonic: {phrase: ETHLANCE_DEPLOY_SEED},
                                     providerOrUrl: ETHLANCE_ETH_NODE_ADDRESS ||  'https://sepolia.base.org'})},
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: 84532,
      from: ETHLANCE_DEPLOYER_ADDRESS
    },
    "production": {
      provider: function () {
        return new HDWalletProvider({mnemonic: {phrase: ETHLANCE_DEPLOY_SEED},
                                    providerOrUrl: `https://base-mainnet.infura.io/v3/${ETHLANCE_INFURA_API_KEY}`})},
      gas: 3000000,
      gasPrice: 0.004 * 1e9,
      network_id: 8453,
      from: ETHLANCE_DEPLOYER_ADDRESS,
      timeoutBlocks: 200,
      confirmations: 2
    }
  }
}
