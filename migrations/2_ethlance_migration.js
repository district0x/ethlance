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

const forwarderTargetPlaceholder = "beefbeefbeefbeefbeefbeefbeefbeefbeefbeef";
const secondForwarderTargetPlaceholder = "dabadabadabadabadabadabadabadabadabadaba";
const thirdForwarderTargetPlaceholder = "dbdadbadbabdabdbadabdbafffd1234fdfadbccc";
const fourthForwarderTargetPlaceholder = "beefabeefabeefabeefabeefabeefabeefabeefa";
const districtConfigPlaceholder = "abcdabcdabcdabcdabcdabcdabcdabcdabcdabcd";
const registryPlaceholder = "dabbdabbdabbdabbdabbdabbdabbdabbdabbdabb";


//
// Contract Artifacts
//

let DSGuard = requireContract("DSGuard");
let EthlanceRegistry = requireContract("EthlanceRegistry");
let EthlanceUser = requireContract("EthlanceUser");
let EthlanceUserFactory = requireContract("EthlanceUserFactory");
let EthlanceUserFactoryForwarder = requireContract("MutableForwarder", "EthlanceUserFactoryForwarder");
let EthlanceComment = requireContract("EthlanceComment");
let EthlanceFeedback = requireContract("EthlanceFeedback");
let EthlanceInvoice = requireContract("EthlanceInvoice");
let EthlanceDispute = requireContract("EthlanceDispute");
let EthlanceTokenStore = requireContract("EthlanceTokenStore");
let EthlanceWorkContract = requireContract("EthlanceWorkContract");
let EthlanceJobStore = requireContract("EthlanceJobStore");
let EthlanceJobFactory = requireContract("EthlanceJobFactory");
let EthlanceJobFactoryForwarder = requireContract("MutableForwarder", "EthlanceJobFactoryForwarder");
let TestToken = requireContract("TestToken");


//
// Deployment Functions
//


/*
  Performs a deployment of the DSGuard
 */
async function deploy_DSGuard(deployer, opts) {
  console.log("Deploying DSGuard");
  await deployer.deploy(DSGuard, opts);
  const instance = await DSGuard.deployed();

  // Set DSGuard Authority
  console.log("- Configuring DSGuard Authority...");
  await instance.setAuthority(instance.address, Object.assign(opts, {gas: 0.5e6}));
  
  // Attach to our smart contract listings
  assignContract(instance, "DSGuard", "ds-guard");
}


async function deploy_EthlanceRegistry(deployer, opts) {
  console.log("Deploying Ethlance Registry");
  await deployer.deploy(EthlanceRegistry, Object.assign(opts, {gas: 3e6}));
  const instance = await EthlanceRegistry.deployed();

  // Set DSGuard Authority
  console.log("- Configuring DSGuard Authority...");
  let dsGuard = await DSGuard.deployed();
  await instance.setAuthority(dsGuard.address, Object.assign(opts, {gas: 0.5e6}));

  // Attach to our smart contract listings
  assignContract(instance, "EthlanceRegistry", "ethlance-registry");
}


async function deploy_EthlanceUser(deployer, opts) {
  console.log("Deploying Ethlance User");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceUser, registryPlaceholder, ethlanceRegistry.address);

  // Deploy
  console.log("- Deploying...");
  await deployer.deploy(EthlanceUser, Object.assign(opts, {gas: 3e6}));
  const instance = await EthlanceUser.deployed();
  
  // Attach
  assignContract(instance, "EthlanceUser", "ethlance-user");
}


async function deploy_EthlanceUserFactory(deployer, opts) {
  console.log("Deploying Ethlance User Factory");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceUserFactory, registryPlaceholder, ethlanceRegistry.address);
  
  console.log("- Linking EthlanceUser...");
  let ethlanceUser = await EthlanceUser.deployed();
  linkBytecode(EthlanceUserFactory, forwarderTargetPlaceholder, ethlanceUser.address);

  console.log("- Deploying EthlanceUserFactory...");
  await deployer.deploy(EthlanceUserFactory, Object.assign(opts, {gas: 3e6}));
  let ethlanceUserFactory = await EthlanceUserFactory.deployed();
  
  console.log("- Linking EthlanceUserFactory --> EthlanceUserFactoryForwarder...");
  linkBytecode(EthlanceUserFactoryForwarder, forwarderTargetPlaceholder, ethlanceUserFactory.address);
  
  console.log("- Deploying EthlanceUserFactoryForwarder...");
  await deployer.deploy(EthlanceUserFactoryForwarder, Object.assign(opts, {gas: 3e6}));
  let ethlanceUserFactoryForwarder = await EthlanceUserFactoryForwarder.deployed();
  
  console.log("- Configuring DSGuard Authority...");
  let dsGuard = await DSGuard.deployed();
  await ethlanceUserFactoryForwarder.setAuthority(dsGuard.address, Object.assign(opts, {gas: 0.5e6}));
  
  //
  // Privileges
  //

  // DSGuard Permissions
  const ANY = await dsGuard.ANY();

  console.log("- DSGuard - Permit ANY --> EthlanceUserFactoryForwarder...");
  await dsGuard.permit(ANY,
                       ethlanceUserFactoryForwarder.address,
                       ANY);

  console.log("- DSGuard - Permit EthlanceRegistry --> EthlanceUserFactoryForwarder...");
  await dsGuard.permit(ethlanceUserFactoryForwarder.address,
                       ethlanceRegistry.address,
                       ANY);

  
  console.log("- Permitting EthlanceUserFactory Factory Privilege");
  await ethlanceRegistry.permitFactoryPrivilege(EthlanceUserFactoryForwarder.address);

  // Assign
  assignContract(ethlanceUserFactory, "EthlanceUserFactory", "ethlance-user-factory");
  assignContract(ethlanceUserFactoryForwarder, "MutableForwarder", "ethlance-user-factory-fwd",
                 {forwards_to: "ethlance-user-factory"});
}


async function deploy_EthlanceComment(deployer, opts) {
  console.log("Deploying EthlanceComment");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceComment, registryPlaceholder, ethlanceRegistry.address);
  
  console.log("- Deploying...");
  await deployer.deploy(EthlanceComment, Object.assign(opts, {gas: 3e6}));
  let ethlanceComment = await EthlanceComment.deployed();

  // Assign
  assignContract(ethlanceComment, "EthlanceComment", "ethlance-comment");
}


async function deploy_EthlanceFeedback(deployer, opts) {
  console.log("Deploying EthlanceFeedback");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceFeedback, registryPlaceholder, ethlanceRegistry.address);
  
  console.log("- Deploying...");
  await deployer.deploy(EthlanceFeedback, Object.assign(opts, {gas: 3e6}));
  let ethlanceFeedback = await EthlanceFeedback.deployed();

  // Assign
  assignContract(ethlanceFeedback, "EthlanceFeedback", "ethlance-feedback");
}


async function deploy_EthlanceInvoice(deployer, opts) {
  console.log("Deploying EthlanceInvoice");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceInvoice, registryPlaceholder, ethlanceRegistry.address);

  console.log("- Linking EthlanceComment...");
  let ethlanceComment = await EthlanceComment.deployed();
  linkBytecode(EthlanceInvoice, forwarderTargetPlaceholder, ethlanceComment.address);

  console.log("- Deploying...");
  await deployer.deploy(EthlanceInvoice, Object.assign(opts, {gas: 3e6}));
  let ethlanceInvoice = await EthlanceInvoice.deployed();

  // Assign
  assignContract(ethlanceInvoice, "EthlanceInvoice", "ethlance-invoice");
}

async function deploy_EthlanceDispute(deployer, opts) {
  console.log("Deploying EthlanceInvoice");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceDispute, registryPlaceholder, ethlanceRegistry.address);

  console.log("- Linking EthlanceComment...");
  let ethlanceComment = await EthlanceComment.deployed();
  linkBytecode(EthlanceDispute, forwarderTargetPlaceholder, ethlanceComment.address);

  console.log("- Linking EthlanceFeedback...");
  let ethlanceFeedback = await EthlanceFeedback.deployed();
  linkBytecode(EthlanceDispute, secondForwarderTargetPlaceholder, ethlanceFeedback.address);

  console.log("- Deploying...");
  await deployer.deploy(EthlanceDispute, Object.assign(opts, {gas: 3.5e6}));
  let ethlanceDispute = await EthlanceDispute.deployed();

  // Assign
  assignContract(ethlanceDispute, "EthlanceDispute", "ethlance-dispute");
}


async function deploy_EthlanceTokenStore(deployer, opts) {
  console.log("Deploying EthlanceTokenStore");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceTokenStore, registryPlaceholder, ethlanceRegistry.address);

  console.log("- Deploying...");
  await deployer.deploy(EthlanceTokenStore, Object.assign(opts, {gas: 2e6}));
  let ethlanceTokenStore = await EthlanceTokenStore.deployed();
  
  // Assign
  assignContract(ethlanceTokenStore, "EthlanceTokenStore", "ethlance-token-store");
}


async function deploy_EthlanceWorkContract(deployer, opts) {
  console.log("Deploying EthlanceWorkContract");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceWorkContract, registryPlaceholder, ethlanceRegistry.address);  

  console.log("- Linking EthlanceInvoice...");
  let ethlanceInvoice = await EthlanceInvoice.deployed();
  linkBytecode(EthlanceWorkContract, forwarderTargetPlaceholder, ethlanceInvoice.address);

  console.log("- Linking EthlanceDispute...");
  let ethlanceDispute = await EthlanceDispute.deployed();
  linkBytecode(EthlanceWorkContract, secondForwarderTargetPlaceholder, ethlanceDispute.address);

  console.log("- Linking EthlanceComment...");
  let ethlanceComment = await EthlanceComment.deployed();
  linkBytecode(EthlanceWorkContract, thirdForwarderTargetPlaceholder, ethlanceComment.address);

  console.log("- Linking EthlanceFeedback...");
  let ethlanceFeedback = await EthlanceFeedback.deployed();
  linkBytecode(EthlanceWorkContract, fourthForwarderTargetPlaceholder, ethlanceFeedback.address);

  console.log("- Deploying...");
  await deployer.deploy(EthlanceWorkContract, Object.assign(opts, {gas: 5e6}));
  let ethlanceWorkContract = await EthlanceWorkContract.deployed();

  // Assign
  assignContract(ethlanceWorkContract, "EthlanceWorkContract", "ethlance-work-contract");
}


async function deploy_EthlanceJobStore(deployer, opts) {
  console.log("Deploying EthlanceJobStore");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceJobStore, registryPlaceholder, ethlanceRegistry.address);
  
  console.log("- Linking EthlanceWorkContract...");
  let ethlanceWorkContract = await EthlanceWorkContract.deployed();
  linkBytecode(EthlanceJobStore, forwarderTargetPlaceholder, ethlanceWorkContract.address);

  console.log("- Linking EthlanceTokenStore...");
  let ethlanceTokenStore = await EthlanceTokenStore.deployed();
  linkBytecode(EthlanceJobStore, secondForwarderTargetPlaceholder, ethlanceTokenStore.address);

  console.log("- Deploying...");
  await deployer.deploy(EthlanceJobStore, Object.assign(opts, {gas: 5e6}));
  let ethlanceJobStore = await EthlanceJobStore.deployed();

  // Assign
  assignContract(ethlanceJobStore, "EthlanceJobStore", "ethlance-job-store");
}


async function deploy_EthlanceJobFactory(deployer, opts) {
  console.log("Deploying EthlanceJobFactory");
  console.log("- Linking Ethlance Registry...");
  let ethlanceRegistry = await EthlanceRegistry.deployed();
  linkBytecode(EthlanceJobFactory, registryPlaceholder, ethlanceRegistry.address); 
 
  console.log("- Linking EthlanceJobStore...");
  let ethlanceJobStore = await EthlanceJobStore.deployed();
  linkBytecode(EthlanceJobFactory, forwarderTargetPlaceholder, ethlanceJobStore.address);

  console.log("- Deploying EthlanceJobFactory...");
  await deployer.deploy(EthlanceJobFactory, Object.assign(opts, {gas: 3.5e6}));
  let ethlanceJobFactory = await EthlanceJobFactory.deployed();

  console.log("- Linking EthlanceJobFactory to EthlanceJobFactoryForwarder...");
  linkBytecode(EthlanceJobFactoryForwarder, forwarderTargetPlaceholder, ethlanceJobFactory.address);

  console.log("- Deploying EthlanceJobFactoryForwarder...");
  await deployer.deploy(EthlanceJobFactoryForwarder, Object.assign(opts, {gas: 3.0e6}));
  let ethlanceJobFactoryForwarder = await EthlanceJobFactoryForwarder.deployed();

  console.log("- Configuring DSGuard Authority...");
  let dsGuard = await DSGuard.deployed();
  await ethlanceJobFactoryForwarder.setAuthority(dsGuard.address, Object.assign(opts, {gas: 0.5e6}));

  //
  // Privileges
  //

  // DSGuard Permissions
  const ANY = await dsGuard.ANY();

  console.log("- DSGuard - Permit ANY --> EthlanceJobFactoryForwarder...");
  await dsGuard.permit(ANY,
                       ethlanceJobFactoryForwarder.address,
                       ANY);

  console.log("- DSGuard - Permit EthlanceRegistry --> EthlanceJobFactoryForwarder...");
  await dsGuard.permit(ethlanceJobFactoryForwarder.address,
                       ethlanceRegistry.address,
                       ANY);

  console.log("- Permitting EthlanceJobFactory Factory Privilege");
  await ethlanceRegistry.permitFactoryPrivilege(EthlanceJobFactoryForwarder.address);

  // Assign
  assignContract(ethlanceJobFactory, "EthlanceJobFactory", "ethlance-job-factory");
  assignContract(ethlanceJobFactoryForwarder, "MutableForwarder", "ethlance-job-factory-fwd",
                 {forwards_to: "ethlance-job-factory"});
}


async function deploy_TestToken(deployer, opts) {
  console.log("Deploying TestToken...");
  await deployer.deploy(TestToken, opts.from, Object.assign(opts, {gas: 3.0e6}));
  let token = await TestToken.deployed();

  // Assign
  assignContract(token, "TestToken", "token");
}


/*
  Deploy All Ethlance Contracts
 */
async function deploy_all(deployer, opts) {
  await deploy_DSGuard(deployer, opts);
  await deploy_EthlanceRegistry(deployer, opts);
  await deploy_EthlanceUser(deployer, opts);
  await deploy_EthlanceUserFactory(deployer, opts);
  await deploy_EthlanceComment(deployer, opts);
  await deploy_EthlanceFeedback(deployer, opts);
  await deploy_EthlanceInvoice(deployer, opts);
  await deploy_EthlanceDispute(deployer, opts);
  await deploy_EthlanceTokenStore(deployer, opts);
  await deploy_EthlanceWorkContract(deployer, opts);
  await deploy_EthlanceJobStore(deployer, opts);
  await deploy_EthlanceJobFactory(deployer, opts);
  await deploy_TestToken(deployer, opts);
  writeSmartContracts();
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
