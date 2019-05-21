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
  return `(ns memefactory.shared.smart-contracts-${env})

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
      edn.kw(":forwards-to"), opts.forward_to,
    ]);
  }

  return [
    edn.kw(clj_contract_name),
    new edn.Map(entry_value),
  ];
};


module.exports = {
  last: last,
  copy: copy,
  linkBytecode: linkBytecode,
  smartContractsTemplate: smartContractsTemplate,
  encodeContractEDN: encodeContractEDN,
};
