const edn = require ("jsedn");
const {env, smart_contracts_path, contracts_build_directory} = require ('../truffle-config.js');
const {copy, encodeContractEDN, readSmartContractsFile, getSmartContractAddress, setSmartContractAddress, writeSmartContracts, setSmartContractForwardsTo} = require("./utils");

const MutableForwarder = artifacts.require("MutableForwarder");
const EthlanceImpl = artifacts.require("Ethlance");

const smartContracts = readSmartContractsFile(smart_contracts_path);
const ethlanceProxyAddress = getSmartContractAddress(smartContracts, ":ethlance");
const jobAddress = getSmartContractAddress(smartContracts, ":job");
const ethlanceStructsAddress = getSmartContractAddress(smartContracts, ":ethlance-structs");

const EthlanceStructs = artifacts.require("EthlanceStructs");

async function deploy_EthlanceImpl(deployer, opts) {
  copy("Ethlance", "EthlanceImpl", contracts_build_directory);
  let ethlanceStructsInstance = await EthlanceStructs.at(ethlanceStructsAddress);
  deployer.link(ethlanceStructsInstance, EthlanceImpl);
  let ethlanceImpl = await deployer.deploy(EthlanceImpl, {...opts, gas: 6e6});
  console.log(">>> initializing ethlanceImpl", jobAddress);
  // await ethlanceImpl.initialize(jobAddress);
  let ethlanceProxy = await MutableForwarder.at(ethlanceProxyAddress);
  await ethlanceProxy.setTarget(ethlanceImpl.address);

  ethlanceThroughProxy = await EthlanceImpl.at(ethlanceProxyAddress);
  ethlanceThroughProxy.initialize(jobAddress);

  [ethlanceImplKey, ethlanceImplValue] = encodeContractEDN(ethlanceImpl, "EthlanceImpl", "ethlance-impl");
  console.log("Setting Ethlance proxy to point to Ethlance implementation at", ethlanceImpl.address);
  setSmartContractForwardsTo(smartContracts, ":ethlance", ":ethlance-impl");
  smartContracts.set(ethlanceImplKey, ethlanceImplValue);
}

module.exports = async function(deployer, network, accounts) {
  const address = accounts[6]; // 6th address
  const gas = 4e6;
  const opts = {gas: gas, from: address};
  await deploy_EthlanceImpl(deployer, opts);
  writeSmartContracts(smart_contracts_path, smartContracts, env);
  console.log("Ethlance initial implementation Deployed!")
}
