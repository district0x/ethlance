var Migrations = artifacts.require("Migrations");

module.exports = function(deployer, network, accounts) {
  deployer.deploy(Migrations, {gas: 500000, from: accounts[0]});
};
