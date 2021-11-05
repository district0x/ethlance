// SPDX-License-Identifier: UNLICENSED
pragma solidity ^0.8.0;

import "./DelegateProxy.sol";
import "./ds-auth/auth.sol";

/**
 * @title Forwarder proxy contract with editable target
 *
 * @dev For TCR Registry contracts (Registry.sol, ParamChangeRegistry.sol) we use mutable forwarders instead of using
 * contracts directly. This is for better upgradeability. Since registry contracts fire all events related to registry
 * entries, we want to be able to access whole history of events always on the same address. Which would be address of
 * a MutableForwarder. When a registry contract is replaced with updated one, mutable forwarder just replaces target
 * and all events stay still accessible on the same address.
 */

contract MutableForwarder is DelegateProxy, DSAuth {

  address public target = 0xBEeFbeefbEefbeEFbeEfbEEfBEeFbeEfBeEfBeef; // checksumed to silence warning

  /**
   * @dev Replaces targer forwarder contract is pointing to
   * Only authenticated user can replace target

   * @param _target New target to proxy into
   */
  function setTarget(address _target) public auth {
    target = _target;
  }

  // Here to silence the compiler warning because in Solidity 0.6 the default fallback
  // function was split into 2 to avoid confusion
  // https://docs.soliditylang.org/en/latest/contracts.html#receive-ether-function
  event Received(address, uint);
  receive() external payable {
    emit Received(msg.sender, msg.value);
  }

  fallback() external payable {
    delegatedFwd(target, msg.data);
  }

}
