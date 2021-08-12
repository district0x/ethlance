/*
  Main Ethlance Deployment Script
 */

const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode} = require("./utils.js");
const fs = require("fs");
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require("../truffle.js");


/*
  Returns the contract artifact for the given `contract_name`
 */
function requireContract(contract_name, contract_copy_name) {
  console.log("Creating Copy of " + contract_name + " for deployment...");
  const copy_name = contract_copy_name || contract_name + "_deployment_copy";
  console.log("- Contract Name: " + copy_name);
  copy(contract_name, copy_name, contracts_build_directory);
  return artifacts.require(copy_name);
}


//
// Placeholders
//

const standardBountiesPlaceholder = "feedfeedfeedfeedfeedfeedfeedfeedfeedfeed";
const ethlanceJobsPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

//
// Contract Artifacts
//

// let DSGuard = requireContract("DSGuard");
// let TestToken = requireContract("TestToken");
// let StandardBounties = requireContract("StandardBounties");
// let EthlanceJobs = requireContract("EthlanceJobs");
// let EthlanceIssuer = requireContract("EthlanceIssuer");

//
// Deployment Functions
//


/*
  Performs a deployment of the DSGuard
 */
async function deploy_DSGuard(deployer, opts) {
  console.log("Deploying DSGuard");
  await deployer.deploy(DSGuard, opts);
  const dsGuard = await DSGuard.deployed();

  // Set DSGuard Authority
  console.log("- Configuring DSGuard Authority...");
  await dsGuard.setAuthority(dsGuard.address, Object.assign(opts, {gas: 0.5e6}));

  // Attach to our smart contract listings
  assignContract(dsGuard, "DSGuard", "ds-guard");
}


async function deploy_TestToken(deployer, opts) {
  console.log("Deploying TestToken...");
  await deployer.deploy(TestToken, opts.from, Object.assign(opts, {gas: 3.0e6}));
  let token = await TestToken.deployed();

  // Assign
  assignContract(token, "TestToken", "token");
}

async function deploy_EthlanceIssuer(deployer, opts){
  let standardBounties = await deployer.deploy(StandardBounties, Object.assign(opts, {gas: 6.4e6}));
  assignContract(standardBounties, "StandardBounties", "standard-bounties");

  let ethlanceJobs = await deployer.deploy(EthlanceJobs, Object.assign(opts, {gas: 7e6}));
  assignContract(ethlanceJobs, "EthlanceJobs", "ethlance-jobs");

  linkBytecode(EthlanceIssuer, standardBountiesPlaceholder, standardBounties.address);
  linkBytecode(EthlanceIssuer, ethlanceJobsPlaceholder, ethlanceJobs.address);

  var ethlanceIssuer = await deployer.deploy(EthlanceIssuer, Object.assign(opts, {gas: 6e6}));

  assignContract(ethlanceIssuer, "EthlanceIssuer", "ethlance-issuer");
}

/*
  Deploy All Ethlance Contracts
 */
async function deploy_all(deployer, opts) {

  console.log("Skipping everything in 2_ethlance_migrations");
  // await deploy_DSGuard(deployer, opts);
  // await deploy_TestToken(deployer, opts);
  // await deploy_EthlanceIssuer(deployer, opts);
  // writeSmartContracts();
}


//
// Smart Contract Functions
//


let smart_contract_listing = [];
/*
  Concatenate the given contract to our smart contract listing.
 */
function assignContract(contract_instance, contract_name, contract_key, opts) {
  console.log("- Assigning '" + contract_name + "' to smart contract listing...");
  opts = opts || {};
  smart_contract_listing = smart_contract_listing.concat(
    encodeContractEDN(contract_instance, contract_name, contract_key, opts));
}

/*
  Write out our smart contract listing to the file defined by `smart_contracts_path`
 */
function writeSmartContracts() {
  console.log("Final Smart Contract Listing:");
  const smart_contracts = edn.encode(new edn.Map(smart_contract_listing));
  console.log(smart_contracts);
  console.log("Writing to smart contract file: " + smart_contracts_path + " ...");
  fs.writeFileSync(smart_contracts_path, smartContractsTemplate(smart_contracts, env));
}


//
// Begin Migration
//


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
