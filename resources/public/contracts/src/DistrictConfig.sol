pragma solidity ^0.4.24;

contract DistrictConfig {
    string public name;

    constructor(string _name) {
	name = _name;
    }
}
