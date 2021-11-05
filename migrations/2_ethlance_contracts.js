// Ethlance unified contract deploy script
const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode, requireContract} = require("./utils.js");
const web3 = require("web3");
const fs = require("fs");
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require("../truffle.js");

// Placeholders
const ethlanceJobsPlaceholder = "deaddeaddeaddeaddeaddeaddeaddeaddeaddead";

// Contract Artifacts
// let DSGuard = requireContract("DSGuard", contracts_build_directory);
// let TestToken = requireContract("TestToken", contracts_build_directory);
// let Ethlance = requireContract("Ethlance", contracts_build_directory);
// let Job = requireContract("Job", contracts_build_directory);

// Require directly without making copies
let DSGuard = artifacts.require("DSGuard");
let TestToken = artifacts.require("TestToken");
let TestNft = artifacts.require("TestNft");
let TestMultiToken = artifacts.require("TestMultiToken");
let Ethlance = artifacts.require("Ethlance");
let Job = artifacts.require("Job");
let EthlanceStructs = artifacts.require("EthlanceStructs");

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
  await dsGuard.setAuthority(dsGuard.address, {...opts, gas: 0.5e6});

  // Attach to our smart contract listings
  assignContract(dsGuard, "DSGuard", "ds-guard");
}

async function deploy_TestNft(deployer, opts) {
  console.log("Deploying TestNft...");
  await deployer.deploy(TestNft, opts.from, {...opts, gas: 3.0e6});
  let token = await TestNft.deployed();

  // Assign
  assignContract(token, "TestNft", "test-nft");
}

async function deploy_TestMultiToken(deployer, opts) {
  console.log("Deploying TestMultiToken...");
  await deployer.deploy(TestMultiToken, opts.from, {...opts, gas: 3.0e6});
  let token = await TestMultiToken.deployed();

  // Assign
  assignContract(token, "TestMultiToken", "test-multi-token");
}

async function deploy_TestToken(deployer, opts) {
  console.log("Deploying TestToken...");
  await deployer.deploy(TestToken, opts.from, {...opts, gas: 3.0e6});
  let token = await TestToken.deployed();

  // Assign
  assignContract(token, "TestToken", "token");
}

async function deploy_EthlanceStructs(deployer, opts){
  let ethlanceStructs = await deployer.deploy(EthlanceStructs, {...opts, gas: 6e6});
  assignContract(ethlanceStructs, "EthlanceStructs", "ethlance-structs");
}

async function deploy_Ethlance(deployer, opts){
  deployer.link(EthlanceStructs, Ethlance);
  let ethlance = await deployer.deploy(Ethlance, {...opts, gas: 6e6});
  assignContract(ethlance, "Ethlance", "ethlance");
}

async function deploy_Job(deployer, opts){
  deployer.link(EthlanceStructs, Job);
  let job = await deployer.deploy(Job, {...opts, gas: 8e6});
  assignContract(job, "Job", "job");
}

async function deploy_all(deployer, opts) {
  await deploy_DSGuard(deployer, opts);
  await deploy_TestToken(deployer, opts);
  await deploy_TestNft(deployer, opts);
  await deploy_TestMultiToken(deployer, opts);
  await deploy_EthlanceStructs(deployer, opts);
  await deploy_Ethlance(deployer, opts);
  await deploy_Job(deployer, opts);
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
  console.log("@@@ using Web3 version:", web3.version);
  console.log("@@@ using address", address);

  try {
    await deploy_all(deployer, opts);
    writeSmartContracts();
    console.log("Ethlance Deployment Finished!");
  }
  catch(error) {
    console.error("ERROR: There was a problem during deployment");
    console.error(error);
  }
};
