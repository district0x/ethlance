const fs = require("fs");
const edn = require ("jsedn");


let last = (array) => {
  return array[array.length - 1];
};


let copy = (srcName, dstName, contracts_build_directory) => {

  let buildPath = contracts_build_directory;

  const srcPath = buildPath + srcName + ".json";
  const dstPath = buildPath + dstName + ".json";

  const data = require(srcPath);
  data.contractName = dstName;

  fs.writeFileSync(dstPath, JSON.stringify(data, null, 2), {flag: "w"});
};


let linkBytecode = (contract, placeholder, replacement) => {
  placeholder = placeholder.replace("0x", "");
  replacement = replacement.replace("0x", "");
  let bytecode = contract.bytecode.split(placeholder).join(replacement);
  contract.bytecode = bytecode;
};


let smartContractsTemplate = (map, env) => {
  return `(ns ethlance.shared.smart-contracts-${env})

(def smart-contracts
  ${map})
`;
};


let encodeContractEDN = (contract_instance, contract_name, contract_key, opts) => {
  const clj_contract_name = ":" + contract_key;
  const contract_address = contract_instance.address;
  opts = opts || {};

  let entry_value = [
    edn.kw(":name"), contract_name,
    edn.kw(":address"), contract_address,
  ];

  // assign a forwards-to optional
  if (opts.forwards_to !== undefined) {
    entry_value = entry_value.concat([
      edn.kw(":forwards-to"), opts.forwards_to,
    ]);
  }

  return [
    edn.kw(clj_contract_name),
    new edn.Map(entry_value),
  ];
};


/*
  Returns the contract artifact for the given `contract_name`
 */
function requireContract(contract_name, contracts_build_directory, contract_copy_name) {
  console.log("Creating Copy of " + contract_name + " for deployment...");
  const copy_name = contract_copy_name || contract_name + "_deployment_copy";
  console.log("- Contract Name: " + copy_name);
  copy(contract_name, copy_name, contracts_build_directory);
  return artifacts.require(copy_name);
}

function readSmartContractsFile(smartContractsPath) {
  var content = fs.readFileSync(smartContractsPath, "utf8");

  content = content.replace(/\(ns.*\)/gm, "");
  content = content.replace(/\(def smart-contracts/gm, "");
  content = content.replace(/\)$/gm, "");

  return edn.parse(content);
}

function setSmartContractAddress(smartContracts, contractKey, newAddress) {
  var contract = edn.atPath(smartContracts, contractKey);
  contract = contract.set(edn.kw(":address"), newAddress);
  return smartContracts.set(edn.kw(contractKey), contract);
}

function setSmartContractForwardsTo(smartContracts, contractKey, targetContractName) {
  var contract = edn.atPath(smartContracts, contractKey);
  contract = contract.set(edn.kw(":forwards-to"), edn.kw(targetContractName));
  return smartContracts.set(edn.kw(contractKey), contract);
}

function getSmartContractAddress(smartContracts, contractKey) {
  try {
    return edn.atPath(smartContracts, contractKey + " :address");
  } catch (e) {
    return null;
  }
}

function encodeSmartContracts (smartContracts) {
  if (Array.isArray(smartContracts)) {
    smartContracts = new edn.Map(smartContracts);
  }
  var contracts = edn.encode(smartContracts);
  console.log(contracts);
  return contracts;
};

function writeSmartContracts(smartContractsPath, smartContracts, env) {
  console.log("Writing to smart contract file: " + smartContractsPath);
  fs.writeFileSync(smartContractsPath, smartContractsTemplate(encodeSmartContracts(smartContracts), env));
}

module.exports = {
  last: last,
  copy: copy,
  linkBytecode: linkBytecode,
  smartContractsTemplate: smartContractsTemplate,
  encodeContractEDN: encodeContractEDN,
  requireContract: requireContract,
  getSmartContractAddress: getSmartContractAddress,
  setSmartContractAddress: setSmartContractAddress,
  readSmartContractsFile: readSmartContractsFile,
  writeSmartContracts: writeSmartContracts,
  encodeSmartContracts: encodeSmartContracts,
  setSmartContractForwardsTo: setSmartContractForwardsTo
};
