// Ethlance unified contract deploy script
const {copy, smartContractsTemplate, encodeContractEDN, linkBytecode, requireContract} = require("./utils.js");
const web3 = require("web3");
const fs = require("fs");
const edn = require("jsedn");
const {env, contracts_build_directory, smart_contracts_path, parameters} = require("../truffle-config.js");

let TestToken = artifacts.require("TestToken");
let TestNft = artifacts.require("TestNft");
let TestMultiToken = artifacts.require("TestMultiToken");
let Ethlance = artifacts.require("Ethlance");
let JobHelpers = artifacts.require("JobHelpers");
let Job = artifacts.require("Job");
let EthlanceStructs = artifacts.require("EthlanceStructs");
let EthlanceProxy = artifacts.require("EthlanceProxy");
let MutableForwarder = artifacts.require("MutableForwarder");
// copy("EthlanceProxy", "EthlanceProxy", contracts_build_directory);
// let EthlanceProxy = artifacts.require("EthlanceProxy");

// Deployment Functions

async function deploy_TestNft(deployer, opts) {
  console.log("Deploying TestNft...");
  await deployer.deploy(TestNft, opts.from, {...opts, gas: 10.0e6});
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
  deployer.link(EthlanceStructs, [JobHelpers, Job]);
  assignContract(ethlanceStructs, "EthlanceStructs", "ethlance-structs");
}

async function deploy_JobHelpers(deployer, opts){
  let jobHelpers = await deployer.deploy(JobHelpers, {...opts, gas: 6e6});
  // Linking to Job to be done in deploy_Job()
  // deployer.link(JobHelpers, Job);
  assignContract(jobHelpers, "JobHelpers", "job-helpers");
}

async function deploy_proxies(deployer, opts) {
  let mutableForwarder = await deployer.deploy(MutableForwarder, opts);
  assignContract(mutableForwarder, "MutableForwarder", "mutable-forwarder");

  let ethlanceForwarder = await deployer.deploy(EthlanceProxy, opts);
  assignContract(ethlanceForwarder, "EthlanceProxy", "ethlance");
}

async function deploy_Job(deployer, opts){
  let job = await deployer.deploy(Job, {...opts, gas: 18e6});
  assignContract(job, "Job", "job");
}

async function deploy_all(deployer, opts) {
  await deploy_TestToken(deployer, opts);
  await deploy_TestNft(deployer, opts);
  await deploy_TestMultiToken(deployer, opts);
  await deploy_EthlanceStructs(deployer, opts);
  await deploy_JobHelpers(deployer, opts);
  await deploy_Job(deployer, opts);
  await deploy_proxies(deployer, opts);
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
  console.log("Writing to smart contract file: " + smart_contracts_path + " ...");
  fs.writeFileSync(smart_contracts_path, smartContractsTemplate(smart_contracts, env));
}


//
// Begin Migration
//
module.exports = async function(deployer, network, accounts) {
  const gas = 4e6;
  console.log("Deploying FROM:", deployer.options.from);
  const from = deployer.options.from || accounts[0];
  const opts = {gas: gas, from: from};

  console.log("Ethlance Deployment Started...");
  await deployer;
  console.log("@@@ using Web3 version:", web3.version);

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
