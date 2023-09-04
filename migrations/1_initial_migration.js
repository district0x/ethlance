var Migrations = artifacts.require("Migrations");

module.exports = function(deployer, network, accounts) {
  const address = accounts[6]; // 6th address
  console.log("Migrations deployment from address: ", address);
  deployer.deploy(Migrations, {gas: 500000, from: address});
};
