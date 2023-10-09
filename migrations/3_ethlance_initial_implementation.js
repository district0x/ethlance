const edn = require ("jsedn");
const {env, smart_contracts_path, contracts_build_directory} = require ('../truffle-config.js');
const {copy, encodeContractEDN, readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, setSmartContractForwardsTo, linkBytecode} = require("./utils");

const Ethlance = artifacts.require("Ethlance");
const EthlanceProxy = artifacts.require("EthlanceProxy");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const ethlanceProxyAddress = getSmartContractAddress(smartContracts, ":ethlance");
const jobAddress = getSmartContractAddress(smartContracts, ":job");
const ethlanceStructsAddress = getSmartContractAddress(smartContracts, ":ethlance-structs");

const EthlanceStructs = artifacts.require("EthlanceStructs");

async function deploy_EthlanceImpl(deployer, opts) {
  let ethlanceStructsInstance = await EthlanceStructs.at(ethlanceStructsAddress);
  deployer.link(ethlanceStructsInstance, Ethlance);

  let ethlanceImpl = await deployer.deploy(Ethlance, {...opts, gas: 6e6});
  let ethlanceProxy = await EthlanceProxy.at(ethlanceProxyAddress);
  console.log(">>> Setting EthlanceProxy.target to", ethlanceImpl.address);
  await ethlanceProxy.setTarget(ethlanceImpl.address);

  console.log(">>> Initializing Ethlance with proxy at: ", ethlanceProxyAddress);
  ethlanceThroughProxy = await Ethlance.at(ethlanceProxyAddress);
  console.log(">>> Setting Ethlance.jobProxyTarget to", jobAddress);
  console.log(">>> ethlanceThroughProxy is at", ethlanceThroughProxy.address);
  ethlanceThroughProxy.initialize(jobAddress);

  [ethlanceImplKey, ethlanceImplValue] = encodeContractEDN(ethlanceImpl, "Ethlance", "ethlance-impl");
  setSmartContractForwardsTo(smartContracts, ":ethlance", ":ethlance-impl");
  smartContracts.set(ethlanceImplKey, ethlanceImplValue);
}

module.exports = async function(deployer, network, accounts) {
  const gas = 4e6;
  const opts = {gas: gas};
  await deploy_EthlanceImpl(deployer, opts);
  writeSmartContracts(smart_contracts_path, smartContracts, env);
  console.log("Ethlance initial implementation Deployed!")
}
