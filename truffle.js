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


module.exports = {
  env: ETHLANCE_ENV,
  smart_contracts_path: __dirname + smartContractsPaths [ETHLANCE_ENV],
  contracts_build_directory: __dirname + '/resources/public/contracts/build/',
  parameters : parameters [ETHLANCE_ENV],

  networks: {
    ganache: {
      host: 'localhost',
      port: 8545,
      gas: 6e6, // gas limit
      gasPrice: 20e9, // 20 gwei, default for ganache
      network_id: '*'
    },
  },

  compilers: {
    solc: {
      version: "0.5.8",
    }
  }
}
