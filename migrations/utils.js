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


let toSnakeCase= (s) => {
  let ss = s.substr(0, 1).toLowerCase();
  for (c of s.substr(1)) {
    if (c.match(/[A-Z]/)) {
      ss += "-" + c.toLowerCase();
    }
    else if (c.match(/\s/)) {
      ss += "-";
    }
    else {
      ss += c;
    }
  }
  
  return ss;
};


let encodeContractEDN = (contract_instance, contract_name) => {
  const clj_contract_name = ":" + toSnakeCase(contract_name);
  return new edn.Map([
    edn.kw(clj_contract_name),
    new edn.Map([
      edn.kw(":name"), contract_name,
      edn.kw(":address"), contract_instance.address,
    ]),
  ]);
};


module.exports = {
  last: last,
  copy: copy,
  linkBytecode: linkBytecode,
  smartContractsTemplate: smartContractsTemplate,
  toSnakeCase: toSnakeCase,
  encodeContractEDN: encodeContractEDN,
};
