/*
  Main Ethlance Deployment Script
 */

const {copy, smartContractsTemplate, encodeContractEDN} = require("./utils.js");
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
async function deploy_DSGuard(deployer, opts) {
  await deployer.deploy(DSGuard, opts);
  const instance = await DSGuard.deployed();

  // Set DSGuard Authority
  console.log("Setting DSGuard Authority...");
  await instance.setAuthority(instance.address, Object.assign(opts, {gas: 500000}));
  
  // Attach to our smart contract listings
  assignContract(instance, "DSGuard", "ds-guard");
}


let smart_contract_listing = [];
/*
  Concatenate the given contract to our smart contract listing.
 */
function assignContract(contract_instance, contract_name, contract_key) {
  console.log("- Assigning '" + contract_name + "' to smart contract listing...");
  smart_contract_listing.concat(encodeContractEDN(contract_instance, contract_name, contract_key));
}

/*
  Write out our smart contract listing to the file defined by `smart_contracts_path`
 */
function writeSmartContracts() {
  console.log("Final Smart Contract Listing:");
  console.log(smart_contract_listing);
  const smart_contracts = edn.encode(new edn.Map(smart_contract_listing));
  console.log("Writing to smart contract file: " + smart_contracts_path + " ...");
  fs.writeFileSync(smart_contracts_path, smartContractsTemplate(smart_contracts, env));
}

/*
  Deploy All Ethlance Contracts
 */
async function deploy_all(deployer, opts) {
  await deploy_DSGuard(deployer, opts);
  writeSmartContracts();
}


module.exports = async function(deployer, network, accounts) {
  const address = accounts[0];
  const gas = 4e6;
  const opts = {gas: gas, from: address};

  console.log("Ethlance Deployment Started...");

  await deployer;
  console.log("@@@ using Web3 version:", web3.version.api);
  console.log("@@@ using address", address);

  try {
    await deploy_all(deployer, opts);
    console.log("Ethlance Deployment Finished!");
  }
  catch(error) {
    console.error("ERROR: There was a problem during deployment");
    console.error(error);
  }
};
