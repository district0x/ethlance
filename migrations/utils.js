const fs = require('fs');

const utils = {

  last: (array) => {
    return array[array.length - 1];
  },

  copy: (srcName, dstName, contracts_build_directory, network, address) => {

    let buildPath = contracts_build_directory;

    const srcPath = buildPath + srcName + '.json';
    const dstPath = buildPath + dstName + '.json';

    const data = require(srcPath);
    data.contractName = dstName;

    // Save address when given
    if (network && address) {
      data.networks = {};

      // Copy existing networks
      if (fs.existsSync(dstPath)) {
        const existing = require(dstPath);
        data.networks = existing.networks;
      }

      data.networks[network.toString()] = {
        address: address
      };
    }
    fs.writeFileSync(dstPath, JSON.stringify(data, null, 2), { flag: 'w' });
  },

  linkBytecode: (contract, placeholder, replacement) => {
    var placeholder = placeholder.replace('0x', '');
    var replacement = replacement.replace('0x', '');
    var bytecode = contract.bytecode.split(placeholder).join(replacement);
    contract.bytecode = bytecode;
  },

  smartContractsTemplate: (map, env) => {
    return `(ns memefactory.shared.smart-contracts-${env})

  (def smart-contracts
    ${map})
`;
  }

};

module.exports = utils;
