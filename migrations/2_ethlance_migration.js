/*
  Main Ethlance Deployment Script
 */

const {copy, smartContractsTemplate} = require("./utils.js");
const fs = require("fs");
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require("../truffle.js");


/*
  Returns the contract artifact for the given `contract_name`
 */
function requireContract(contract_name) {
  console.log("Creating Copy of " + contract_name + " for deployment...");
  const copy_name = contract_name + "_deployment_copy";
  copy(contract_name, copy_name, contracts_build_directory);
  return artifacts.require(copy_name);
}


//
// Contract Artifacts
//
let DSGuard = requireContract("DSGuard");



/*
  Performs a deployment of the DSGuard
 */
function deploy_DSGuard(deployer, opts) {
  return deployer.deploy(DSGuard, opts);
}


function deploy_all(deployer, opts) {
  return deploy_DSGuard(deployer, opts);
}


module.exports = function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  deployer.then(() => {
    console.log("@@@ using Web3 version:", web3.version.api);
    console.log("@@@ using address", address);
  });

  deploy_all(deployer, opts).then(() => {
    console.log("Finished Deployment!");
  });
};
