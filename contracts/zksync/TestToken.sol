// SPDX-License-Identifier: GPL-3.0
/*
  This ERC20 compliant contract should not be used as an actual
  cryptocurrency, and should only be used strictly in a development
  and testing environment. Public use of this contract as a store of
  value is not recommended."
*/

pragma solidity ^0.8.0;

import "@openzeppelin/contracts/utils/math/SafeMath.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";

/// @title ERC20 Token Implementation for testing purposes
contract TestToken is IERC20 {
  using SafeMath for uint256;


  //
  // Token Parameters
  //
  string public name = "TestToken";
  string public symbol = "TEST";
  uint8 public decimals = 0;


  //
  // Members
  //
  uint256 private total_balance = 0;
  address private contract_owner;


  //
  // Collections
  //
  mapping (address => uint256) public token_balance_mapping;
  mapping (address => mapping (address => uint256)) private token_allowance_mapping;

  //
  // Constructor
  //
  constructor(address _contract_owner) {
    contract_owner = _contract_owner;
  }


  //
  // ERC20 Implmentation
  //


  function totalSupply() external view override returns (uint256) {
    return total_balance;
  }


  function balanceOf(address _owner) external view override returns (uint256) {
    return token_balance_mapping[_owner];
  }


  function allowance(address owner, address spender)
    external view override returns (uint256) {
    return token_allowance_mapping[owner][spender];
  }


  function transfer(address to, uint256 value) external override returns (bool) {
    if (token_balance_mapping[msg.sender] < value || to == address(0)) {
      return false;
    }

    token_balance_mapping[msg.sender] = token_balance_mapping[msg.sender].sub(value);
    token_balance_mapping[to] = token_balance_mapping[to].add(value);
    emit Transfer(msg.sender, to, value);
    return true;
  }


  function approve(address spender, uint256 value)
    external override returns (bool) {
    if (spender == address(0)) {
      return false;
    }

    token_allowance_mapping[msg.sender][spender] = value;
    emit Approval(msg.sender, spender, value);
    return true;
  }


  function transferFrom(address from, address to, uint256 value)
    external override returns (bool) {
    if (token_allowance_mapping[from][to] < value ||
        token_balance_mapping[from] < value ||
        to == address(0)) {
      return false;
    }

    token_allowance_mapping[from][to] = token_allowance_mapping[from][to].sub(value);
    token_balance_mapping[from] = token_balance_mapping[from].sub(value);
    token_balance_mapping[to] = token_balance_mapping[to].add(value);

    return true;
  }


  //
  // Methods
  //

  function mint(address to, uint256 value)
    external returns(bool) {
    if (to == address(0)) {
      return false;
    }

    total_balance = total_balance.add(value);
    token_balance_mapping[to] = token_balance_mapping[to].add(value);

    return true;
  }
}
